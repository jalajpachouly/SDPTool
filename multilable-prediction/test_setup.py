"""
Test script to verify all required packages are installed and working
"""
import sys

def test_imports():
    """Test all critical imports"""
    try:
        print(f"Python version: {sys.version}")
        print("\nTesting imports...")
        
        import pandas
        print("✓ pandas:", pandas.__version__)
        
        import numpy
        print("✓ numpy:", numpy.__version__)
        
        import sklearn
        print("✓ scikit-learn:", sklearn.__version__)
        
        import tensorflow
        print("✓ tensorflow:", tensorflow.__version__)
        
        import nltk
        print("✓ nltk:", nltk.__version__)
        
        from utils.model_persistence import ModelPersistence
        print("✓ model_persistence module OK")
        
        print("\n✅ All packages installed successfully!")
        print("\n📁 Check saved models at: multilable-prediction/models/")
        return True
        
    except ImportError as e:
        print(f"\n❌ Missing package: {e}")
        print("\nRun: pip install -r requirements.txt")
        return False

if __name__ == "__main__":
    success = test_imports()
    sys.exit(0 if success else 1)
