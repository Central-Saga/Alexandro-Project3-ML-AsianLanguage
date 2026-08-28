import unittest
import pandas as pd
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from src.predict import predict, predict_with_details

class TestPredict(unittest.TestCase):
    
    def test_predict_success(self):
        # Input DataFrame with text column
        df = pd.DataFrame({
            "text": [
                "Ini adalah contoh kalimat bahasa Indonesia.",
                "This is not English but the model will guess."
            ]
        })
        
        preds = predict(df)
        self.assertIsInstance(preds, list)
        self.assertEqual(len(preds), len(df))
        
    def test_predict_with_details(self):
        df = pd.DataFrame({
            "text": ["Ang pelikulang ito ay napakaganda."]
        })
        
        results = predict_with_details(df)
        self.assertIsInstance(results, list)
        self.assertEqual(len(results), 1)
        self.assertIn("label", results[0])
        self.assertIn("margin_score", results[0])
        self.assertIsInstance(results[0]["margin_score"], float)

    def test_missing_text_column(self):
        df = pd.DataFrame({"not_text": ["test"]})
        with self.assertRaisesRegex(ValueError, "DataFrame harus memiliki kolom 'text'"):
            predict(df)
            
    def test_invalid_input_type(self):
        with self.assertRaisesRegex(TypeError, "Input harus berupa pandas DataFrame"):
            predict("Ini string bukan DataFrame")
            
    def test_empty_dataframe(self):
        df = pd.DataFrame({"text": []})
        preds = predict(df)
        self.assertEqual(preds, [])
        
    def test_null_values(self):
        df = pd.DataFrame({"text": [None, "Normal text"]})
        preds = predict(df)
        self.assertEqual(len(preds), 2)

if __name__ == "__main__":
    unittest.main()
