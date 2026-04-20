import urllib.request
import os
import time

def download_sport_images(count, target_dir):
    if not os.path.exists(target_dir):
        os.makedirs(target_dir, exist_ok=True)
        print(f"Created directory: {target_dir}")
    else:
        print(f"Directory already exists: {target_dir}")

    # LoremFlickr provides sport images
    url_base = "https://loremflickr.com/640/480/sport"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3'
    }

    for i in range(1, count + 1):
        try:
            filename = f"sport_item_{i:03d}.jpg"
            filepath = os.path.join(target_dir, filename)
            
            print(f"Downloading image {i}/{count}...")
            # We add a lock/i to URL to avoid caching and get different images
            url = f"{url_base}?lock={i}"
            
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=15) as response:
                if response.status == 200:
                    with open(filepath, 'wb') as f:
                        f.write(response.read())
                    print(f"Saved to {filepath}")
                else:
                    print(f"Failed to download image {i}: HTTP {response.status}")
            
            # Short sleep to be polite
            time.sleep(0.05)
            
        except Exception as e:
            print(f"Error downloading image {i}: {e}")

if __name__ == "__main__":
    count = 100
    target_dir = os.path.join("src", "main", "resources", "static", "images")
    download_sport_images(count, target_dir)
