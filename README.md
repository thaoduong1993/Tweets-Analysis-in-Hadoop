# Tweets-Analysis-in-Hadoop
Analyze a large file of tweet data in JSON format using Hive in Hadoop.

Note: 
I was given the JSON file (tweet). Check my Sentiment Analysis with R to see how I scrape online reviews with R.
Use process.py to transform the tweet data to json format that can use json serde
Tweet-json-hive is actually hive language, not java. I just saved in as java so it can appear all colorful in my Sublime Text

Some interesting things to share:
1. Look at the data carefully
It was a huge file with thousands of rows and dozens of attributes for each tweet. I even discovered 2 other types of tweets (retweeted and quoted) that were nested inside the direct tweets - almost miss them

2. Non-character string can mess up your result (like "\n" for new line)
Look at the Null values, find the inconsistency in the review (that "\n") and use regexp_replace to replace it.

3. Difference between create table and create view
Create view indeed saves me a lot of processing time (especially with Hive) for non-key data tables.

4. SQL, SQL, and SQL
At the end, Hive's syntax is very similar to SQL. I'm glad to re-touch several SQL functions that I just "know" before




