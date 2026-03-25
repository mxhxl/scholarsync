import logging
import time
import requests
import xml.etree.ElementTree as ET
from datetime import date
from app.external.arxiv_client import PaperData
from app.config import settings

logger = logging.getLogger(__name__)

ESEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"
EFETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi"


def _esearch(query: str, max_results: int) -> list[str]:
    params = {
        "db": "pubmed",
        "term": query,
        "retmax": max_results,
        "retmode": "json",
    }
    resp = requests.get(ESEARCH_URL, params=params, timeout=15)
    resp.raise_for_status()
    return resp.json().get("esearchresult", {}).get("idlist", [])


def _efetch(pmids: list[str]) -> str:
    params = {
        "db": "pubmed",
        "id": ",".join(pmids),
        "retmode": "xml",
        "rettype": "abstract",
    }
    resp = requests.get(EFETCH_URL, params=params, timeout=30)
    resp.raise_for_status()
    return resp.text


def _parse_pubmed_xml(xml_text: str) -> list[PaperData]:
    root = ET.fromstring(xml_text)
    papers = []
    for article in root.findall(".//PubmedArticle"):
        try:
            pmid_el = article.find(".//PMID")
            pmid = pmid_el.text if pmid_el is not None else "unknown"

            title_el = article.find(".//ArticleTitle")
            title = title_el.text or "" if title_el is not None else ""

            abstract_el = article.find(".//AbstractText")
            abstract = abstract_el.text or "" if abstract_el is not None else ""

            authors = []
            for author in article.findall(".//Author"):
                last = author.findtext("LastName", "")
                first = author.findtext("ForeName", "")
                if last:
                    authors.append(f"{first} {last}".strip())

            pub_date_el = article.find(".//PubDate")
            pub_date = None
            if pub_date_el is not None:
                year = pub_date_el.findtext("Year")
                month = pub_date_el.findtext("Month", "01")
                day = pub_date_el.findtext("Day", "01")
                try:
                    from datetime import datetime
                    month_int = int(month) if month.isdigit() else datetime.strptime(month, "%b").month
                    pub_date = date(int(year), month_int, int(day))
                except Exception:
                    pass

            journal_el = article.find(".//Journal/Title")
            venue = journal_el.text if journal_el is not None else None

            papers.append(PaperData(
                external_id=f"pubmed:{pmid}",
                source="pubmed",
                title=title,
                authors=authors,
                abstract=abstract,
                published_date=pub_date,
                pdf_url=None,
                citation_count=0,
                venue=venue,
            ))
        except Exception as e:
            logger.warning("Failed to parse PubMed article: %s", e)

    return papers


def fetch_papers(keywords: list[str], max_results: int = None) -> list[PaperData]:
    if max_results is None:
        max_results = settings.PUBMED_MAX_RESULTS

    query = " OR ".join(f'"{kw}"[All Fields]' for kw in keywords)
    try:
        pmids = _esearch(query, max_results)
        if not pmids:
            return []
        time.sleep(0.34)  # Stay under 3 req/s
        xml_text = _efetch(pmids)
        return _parse_pubmed_xml(xml_text)
    except Exception as e:
        logger.error("PubMed fetch error: %s", e)
        return []
