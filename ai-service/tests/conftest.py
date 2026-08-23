"""Makes ai-service/'s flat top-level modules (main, models/, api/, features/)
importable from tests/ regardless of the cwd pytest is invoked from -- same
sys.path.insert pattern already used by models/xgboost_model.py, training/
build_dataset.py, etc. for their own cross-module imports (this project has
no setup.py/pyproject.toml package layout)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
