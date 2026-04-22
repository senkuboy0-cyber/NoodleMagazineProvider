import json

with open('.github/workflows/build.yml', 'r') as f:
    content = f.read()

# Replace the plugins.json content
old_plugins = """[
            {
              "url": "https://raw.githubusercontent.com/senkuboy0-cyber/NoodleMagazineProvider/builds/NoodleMagazineProvider.cs3",
              "internalName": "NoodleMagazineProvider",
              "tvTypes": ["Movie", "Others"],
              "language": "en",
              "name": "NoodleMagazine",
              "authors": ["senkuboy0-cyber"],
              "version": 1,
              "description": "NoodleMagazine - Free Videos",
              "repositoryUrl": "https://github.com/senkuboy0-cyber/NoodleMagazineProvider",
              "status": 1
            }
          ]"""

new_plugins = """[
            {
              "url": "https://raw.githubusercontent.com/senkuboy0-cyber/NoodleMagazineProvider/builds/NoodleMagazineProvider.cs3",
              "internalName": "NoodleMagazineProvider",
              "tvTypes": ["NSFW", "Others"],
              "language": "en",
              "name": "NoodleMagazine",
              "authors": ["senkuboy0-cyber"],
              "version": 2,
              "description": "NoodleMagazine - Free Videos",
              "repositoryUrl": "https://github.com/senkuboy0-cyber/NoodleMagazineProvider",
              "status": 1
            },
            {
              "url": "https://raw.githubusercontent.com/senkuboy0-cyber/NoodleMagazineProvider/builds/PmovieProvider.cs3",
              "internalName": "PmovieProvider",
              "tvTypes": ["Movie", "NSFW", "Others"],
              "language": "en",
              "name": "560pmovie",
              "authors": ["senkuboy0-cyber"],
              "version": 1,
              "description": "560pmovie - Free Videos",
              "repositoryUrl": "https://github.com/senkuboy0-cyber/NoodleMagazineProvider",
              "status": 1
            }
          ]"""

content = content.replace(old_plugins, new_plugins)

with open('.github/workflows/build.yml', 'w') as f:
    f.write(content)
