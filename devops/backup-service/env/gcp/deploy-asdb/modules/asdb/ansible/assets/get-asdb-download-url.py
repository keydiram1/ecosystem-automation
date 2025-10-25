#!/usr/bin/env
import os
import urllib.request
from bs4 import BeautifulSoup
URL="https://download.aerospike.com/artifacts/aerospike-server-enterprise"
DISTRO=os.getenv("DISTRO")
ARCH=os.getenv("ARCH")
ASDB_VERSION=os.getenv("ASDB_VERSION")

def get_asdb_download_link():
    try:
        with urllib.request.urlopen(f"{URL}/{ASDB_VERSION}") as response:
            html_content = response.read().decode("utf-8")
            filenames = set()
            soup = BeautifulSoup(html_content, "html.parser")
            for link in soup.find_all("a", href=True):
                href = link["href"]
                if href.endswith(".tgz"):
                    filenames.add(href)
            filename = [item for item in filenames if DISTRO in item and ARCH in item]
            return f"{URL}/{ASDB_VERSION}/{filename[0]}"
    except urllib.error.HTTPError as e:
        return f"Failed to fetch the URL. Error: {e}"

def main():
    print(get_asdb_download_link())

if __name__ == "__main__":
    main()
