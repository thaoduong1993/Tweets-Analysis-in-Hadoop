add jar Desktop/json-serde-1.3.8-jar-with-dependencies.jar;

// Load all tweet data regardless the type
CREATE TABLE tweet_all (
id BIGINT,
created_at STRING,
entities STRUCT<
hashtags:ARRAY<STRUCT<text:STRING>>>,
text STRING,
user STRUCT<
name:STRING,
followers_count:INT,
location:STRING>,
quoted_status STRUCT<
id: BIGINT,
created_at: STRING,
entities: STRUCT<
hashtags:ARRAY<STRUCT<text:STRING>>>,
text: STRING,
user: STRUCT<
name: STRING,
followers_count: INT,
location: STRING>>,
retweeted_status STRUCT<
id: BIGINT,
created_at: STRING,
entities: STRUCT<
hashtags:ARRAY<STRUCT<text:STRING>>>,
text: STRING,
user: STRUCT<
name: STRING,
followers_count: INT,
location: STRING>>
) 
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES ("ignore.malformed.json" = "true");

load data local inpath 'Desktop/tweet_out.json' into table tweet_all;

// Collapse all 3 types of tweets into 1 table, starting with direct tweet
CREATE table tweets_table as
select id, 
TO_DATE(from_unixtime( unix_timestamp(concat( '2016 ', 
substring(created_at,5,6)), 'yyyy MMM dd'))) as created_at, 
entities.hashtags, regexp_replace(text, '\n', ' ') as text,
user.name as uname, user.followers_count as followers_cnt, user.location as location
from tweet_all;

// Insert quoted_tweets
INSERT INTO tweets_table
select quoted_status.id, 
TO_DATE(from_unixtime( unix_timestamp(concat( '2016 ', 
substring(quoted_status.created_at,5,6)), 'yyyy MMM dd'))) as created_at, 
quoted_status.entities.hashtags,
regexp_replace(quoted_status.text, '\n', ' ') as text,
quoted_status.user.name, quoted_status.user.followers_count, quoted_status.user.location
from tweet_all where quoted_status IS NOT NULL;

// Insert retweeted_tweets
INSERT INTO tweets_table
select retweeted_status.id, 
TO_DATE(from_unixtime( unix_timestamp(concat( '2016 ', 
substring(retweeted_status.created_at,5,6)), 'yyyy MMM dd'))) as created_at, 
retweeted_status.entities.hashtags,
regexp_replace(retweeted_status.text, '\n', ' '), 
retweeted_status.user.name, retweeted_status.user.followers_count, retweeted_status.user.location
from tweet_all where retweeted_status IS NOT NULL;

select count(*) from tweets_table;

// List all the hashtags in order from the most popular to least popular
create view hashtags_view as 
select ht as hashtag,count(ht) as count from tweets_table 
lateral view explode(hashtags.text) dummy as ht 
group by ht order by count desc;

select * from hashtags_view;

// Select state wtih the most active users 

// Use substr because location is like Dallas, TX
create view location_view as 
select id, substr(location,-2) as state 
from tweets_table;

select state, count(id) as tweets_count
from location_view group by state order by tweets_count desc limit 1;

// Select the top ten user with the most followers
select uname, max(followers_cnt) as count from tweets_table
group by uname order by count desc limit 10;

// Calculate sentiment score for each hashtag

// Load the Afinn dictionary
create table dictionary (
   word STRING,
   score INT)
row format delimited
fields terminated by '\t';

load data local inpath 'Desktop/Dictionary.txt' into table dictionary;

// Create a word list
create view text_view as
select id, uname, created_at, words 
from tweets_table lateral view 
explode(sentences(lower(text))) dummy as words;

create view text_view2 as 
select id, uname, created_at, word from text_view 
lateral view explode(words) dummy as word;

// Join the word list with dictionary
create view text_view3 as
select id, uname, created_at, t.word, 
case when score IS NULL then 0 else score end as score
from text_view2 t left outer join dictionary d on t.word = d.word;

// Sum the score for each tweet
create view score_view as
select id, uname, created_at, sum(score) as total_score,
case when sum(score) > 0 then 'positive'
when sum(score) < 0 then 'negative'
else 'neutral' end as sentiment
from text_view3 group by created_at, uname, id order by id desc;
