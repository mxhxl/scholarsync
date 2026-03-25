from pydantic import BaseModel


class DeviceTokenCreate(BaseModel):
    token: str
    platform: str  # android / ios / web


class DeviceTokenResponse(BaseModel):
    id: str
    user_id: str
    token: str
    platform: str
