import logging
from datetime import datetime, timezone
from dateutil.relativedelta import relativedelta
from collections import defaultdict
from bertopic import BERTopic
from bertopic.representation import KeyBERTInspired
from umap import UMAP
from sklearn.feature_extraction.text import CountVectorizer

logger = logging.getLogger(__name__)

# HDBSCAN often fails to build on Windows (needs C++ compiler). Use sklearn fallback when missing.
try:
    from hdbscan import HDBSCAN
    _hdbscan_available = True
except ImportError:
    HDBSCAN = None
    _hdbscan_available = False


def _get_clusterer():
    """Use HDBSCAN if available, else AgglomerativeClustering (works without extra build deps)."""
    if _hdbscan_available and HDBSCAN is not None:
        return HDBSCAN(
            min_cluster_size=10,
            min_samples=5,
            prediction_data=True,
        )
    from sklearn.cluster import AgglomerativeClustering
    return AgglomerativeClustering(n_clusters=15, linkage="ward")


def build_bertopic_model() -> BERTopic:
    umap_model = UMAP(
        n_neighbors=15,
        n_components=5,
        min_dist=0.0,
        metric="cosine",
        random_state=42,
    )
    cluster_model = _get_clusterer()
    vectorizer_model = CountVectorizer(
        ngram_range=(1, 2),
        stop_words="english",
        min_df=3,
    )
    representation_model = KeyBERTInspired()
    return BERTopic(
        umap_model=umap_model,
        hdbscan_model=cluster_model,
        vectorizer_model=vectorizer_model,
        representation_model=representation_model,
        top_n_words=10,
        nr_topics="auto",
        calculate_probabilities=False,
    )


def analyze_trends(papers_with_dates: list[dict]) -> list[dict]:
    """
    Input: [{title, abstract, published_date}]
    Process: fit BERTopic on (title + abstract) strings
    Per topic: calculate monthly counts for last 3 months
    growth_rate = (month3 - month1) / month1 * 100
    trend: >50% = rising, <-30% = declining, else = stable
    Return top 25 topics sorted by |growth_rate| DESC
    """
    if len(papers_with_dates) < 100:
        logger.warning("Not enough papers (%d) for trend analysis", len(papers_with_dates))
        return []

    docs = [
        f"{p.get('title', '')} {p.get('abstract', '')}".strip()
        for p in papers_with_dates
    ]
    dates = [p.get("published_date") for p in papers_with_dates]

    model = build_bertopic_model()
    topics, _ = model.fit_transform(docs)

    now = datetime.now(timezone.utc)
    month_labels = [
        (now - relativedelta(months=2)).strftime("%Y-%m"),
        (now - relativedelta(months=1)).strftime("%Y-%m"),
        now.strftime("%Y-%m"),
    ]

    topic_monthly: dict[int, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for i, topic_id in enumerate(topics):
        if topic_id == -1:
            continue
        pub_date = dates[i]
        if pub_date is None:
            continue
        if hasattr(pub_date, "strftime"):
            month_str = pub_date.strftime("%Y-%m")
        else:
            month_str = str(pub_date)[:7]
        if month_str in month_labels:
            topic_monthly[topic_id][month_str] += 1

    results = []
    topic_info = model.get_topic_info()
    for _, row in topic_info.iterrows():
        tid = row["Topic"]
        if tid == -1:
            continue

        keywords = [kw for kw, _ in model.get_topic(tid)]
        label = row.get("Name", " ".join(keywords[:3]))
        monthly_counts = [topic_monthly[tid].get(m, 0) for m in month_labels]
        papers_count = sum(monthly_counts)

        m1, m3 = monthly_counts[0], monthly_counts[2]
        if m1 > 0:
            growth_rate = (m3 - m1) / m1 * 100
        else:
            growth_rate = 100.0 if m3 > 0 else 0.0

        if growth_rate > 50:
            trend = "rising"
        elif growth_rate < -30:
            trend = "declining"
        else:
            trend = "stable"

        results.append({
            "topic_id": int(tid),
            "keywords": keywords,
            "label": label,
            "papers_count": papers_count,
            "growth_rate": round(growth_rate, 2),
            "trend": trend,
            "monthly_counts": monthly_counts,
        })

    results.sort(key=lambda x: abs(x["growth_rate"]), reverse=True)
    return results[:25]
