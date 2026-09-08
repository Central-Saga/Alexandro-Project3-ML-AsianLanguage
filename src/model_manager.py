import os
import json
import shutil
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
ACTIVE_ARTIFACTS_DIR = BASE_DIR / "artifacts"
CANDIDATE_ARTIFACTS_DIR = BASE_DIR / "artifacts_candidate"
BACKUP_ARTIFACTS_DIR = BASE_DIR / "artifacts_backup"

def promote_candidate(f1_tolerance: float = 0.02, max_class_drop: float = 0.10):
    """
    Mempromosikan model dari artifacts_candidate ke artifacts jika memenuhi kriteria.
    - f1_tolerance: seberapa besar F1 score boleh turun.
    - max_class_drop: seberapa besar F1 per kelas boleh turun.
    """
    if not CANDIDATE_ARTIFACTS_DIR.exists():
        print("Tidak ada model kandidat.")
        return False
        
    candidate_config_path = CANDIDATE_ARTIFACTS_DIR / "config.json"
    if not candidate_config_path.exists():
        print("Model kandidat tidak memiliki config.json.")
        return False
        
    with open(candidate_config_path, "r", encoding="utf-8") as f:
        cand_config = json.load(f)
        
    active_config_path = ACTIVE_ARTIFACTS_DIR / "config.json"
    if active_config_path.exists():
        with open(active_config_path, "r", encoding="utf-8") as f:
            act_config = json.load(f)
            
        cand_f1 = cand_config.get("macro_f1", 0)
        act_f1 = act_config.get("macro_f1", 0)
        
        print(f"Active Macro F1: {act_f1:.4f} | Candidate Macro F1: {cand_f1:.4f}")
        
        if cand_f1 < act_f1 - f1_tolerance:
            print(f"TOLAK: Macro F1 turun terlalu jauh (>{f1_tolerance}).")
            return False
            
        # Cek drop per class
        cand_report = cand_config.get("classification_report", {})
        act_report = act_config.get("classification_report", {})
        
        for lang, metrics in act_report.items():
            if isinstance(metrics, dict) and 'f1-score' in metrics:
                old_f1 = metrics['f1-score']
                new_f1 = cand_report.get(lang, {}).get('f1-score', 0)
                if new_f1 < old_f1 - max_class_drop:
                    print(f"TOLAK: F1 untuk bahasa '{lang}' turun terlalu jauh ({old_f1:.4f} -> {new_f1:.4f}).")
                    return False
    else:
        print("Model aktif tidak ditemukan, kandidat akan dipromosikan langsung.")
        
    print("Mempromosikan kandidat...")
    
    # Buat backup
    if ACTIVE_ARTIFACTS_DIR.exists():
        if BACKUP_ARTIFACTS_DIR.exists():
            shutil.rmtree(BACKUP_ARTIFACTS_DIR)
        shutil.copytree(ACTIVE_ARTIFACTS_DIR, BACKUP_ARTIFACTS_DIR)
        print(f"Backup dibuat di {BACKUP_ARTIFACTS_DIR}")
        shutil.rmtree(ACTIVE_ARTIFACTS_DIR)
        
    shutil.copytree(CANDIDATE_ARTIFACTS_DIR, ACTIVE_ARTIFACTS_DIR)
    print("Kandidat berhasil dipromosikan.")
    return True

def rollback():
    """Mengembalikan artifacts dari backup."""
    if not BACKUP_ARTIFACTS_DIR.exists():
        print("Backup tidak ditemukan.")
        return False
        
    if ACTIVE_ARTIFACTS_DIR.exists():
        shutil.rmtree(ACTIVE_ARTIFACTS_DIR)
        
    shutil.copytree(BACKUP_ARTIFACTS_DIR, ACTIVE_ARTIFACTS_DIR)
    print("Rollback berhasil!")
    return True

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "rollback":
        rollback()
    else:
        promote_candidate()
