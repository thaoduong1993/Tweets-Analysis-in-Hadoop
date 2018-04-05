import json

with open('tweet.json', 'r') as json_file:
    json_data = json.load(json_file)

    with open('tweet_out.json', 'w') as json_out_file:
        for record in json_data:
            json_out_file.write(json.dumps(record) + '\n')