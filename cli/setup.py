from setuptools import setup

setup(
    name="ai-dex2jar",
    version="0.1.0",
    description="AI-friendly CLI wrapper for dex2jar reverse engineering tools",
    py_modules=["ai_dex2jar"],
    scripts=["ai_dex2jar.py"],
    python_requires=">=3.10",
)
