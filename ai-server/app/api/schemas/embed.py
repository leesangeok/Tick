from pydantic import BaseModel


class EmbedResponse(BaseModel):
    upserted: int
