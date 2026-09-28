"""Local embeddings API for the Spring AI client; no external inference requests."""
import os
from typing import Literal

import torch
from fastapi import FastAPI
from pydantic import BaseModel, Field
from transformers import AutoModel, AutoTokenizer

MODEL = "seongyeon1/korean-sbert-korsts"
REVISION = "f466ec3c30fb0e9a038b7a8f05e2298c70d01e08"
source = os.getenv("KLUE_MODEL_DIR") or MODEL
tokenizer = AutoTokenizer.from_pretrained(source, revision=REVISION)
encoder = AutoModel.from_pretrained(source, revision=REVISION).eval()
torch.set_num_threads(int(os.getenv("ENCODER_THREADS", "2")))
app = FastAPI()


class Request(BaseModel):
    model: Literal["seongyeon1/korean-sbert-korsts"]
    input: str | list[str] = Field(min_length=1)
    encoding_format: Literal["float"] = "float"


@app.post("/v1/embeddings")
@torch.inference_mode()
def embeddings(request: Request):
    texts = [request.input] if isinstance(request.input, str) else request.input
    data, tokens = [], 0
    for start in range(0, len(texts), 16):
        batch = tokenizer(texts[start:start + 16], padding=True, truncation=True,
                          max_length=128, return_tensors="pt")
        hidden = encoder(**batch).last_hidden_state
        mask = batch.attention_mask.unsqueeze(-1)
        pooled = (hidden * mask).sum(1) / mask.sum(1).clamp(min=1)
        vectors = torch.nn.functional.normalize(pooled, dim=-1)
        tokens += int(mask.sum())
        for vector in vectors.tolist():
            data.append({"object": "embedding", "index": len(data), "embedding": vector})
    return {"object": "list", "model": MODEL, "data": data,
            "usage": {"prompt_tokens": tokens, "total_tokens": tokens}}
