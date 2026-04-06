from setuptools import setup, find_packages

setup(
    name="android-pyhole-core",
    version="1.0.0",
    packages=find_packages(),
    install_requires=[
        "flask",
        "aiodns",
        "requests",
    ],
    description="Python core for AndroidPyHole",
    author="AndroidPyHole Developer",
)
