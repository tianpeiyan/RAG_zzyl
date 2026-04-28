from __future__ import annotations

import json
from pathlib import Path
from typing import Sequence

from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage, message_to_dict, messages_from_dict


class FileChatMessageHistory(BaseChatMessageHistory):
    """Small file-backed history compatible with LangChain message history."""

    def __init__(self, session_id: str, storage_path: Path) -> None:
        safe_session_id = "".join(
            character if character.isalnum() or character in {"-", "_"} else "_"
            for character in session_id
        )
        self.file_path = storage_path / f"{safe_session_id}.json"
        self.file_path.parent.mkdir(parents=True, exist_ok=True)

    @property
    def messages(self) -> list[BaseMessage]:
        if not self.file_path.exists():
            return []
        try:
            message_dicts = json.loads(self.file_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return []
        return messages_from_dict(message_dicts)

    def add_messages(self, messages: Sequence[BaseMessage]) -> None:
        all_messages = [*self.messages, *messages]
        payload = [message_to_dict(message) for message in all_messages]
        self.file_path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def clear(self) -> None:
        self.file_path.write_text("[]", encoding="utf-8")
