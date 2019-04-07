#!/usr/bin/env bash

cat << EOF > /var/www/podcast/rss.xml
<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:media="http://search.yahoo.com/mrss/">
    <channel>
        <title>Fake Podcast</title>
        <link>http://localhost:7070/feed.rss</link>
        <description>A Fake Podcast to help local dev</description>
        <itunes:subtitle>A Fake Podcast to help local dev</itunes:subtitle>
        <itunes:summary>A Fake Podcast to help local dev</itunes:summary>
        <language>fr-fr</language>
        <itunes:author>RSS</itunes:author>
        <itunes:category />
        <pubDate>Wed, 27 Mar 2019 10:06:26 +0100</pubDate>
        <image>
            <height>1400</height>
            <url>http://localhost:7070/fake.jpg</url>
            <width>1400</width>
        </image>
        <itunes:image>http://localhost:7070/fake.jpg</itunes:image>
EOF

NUMBER_OF_ITEM=50

for i in $(eval echo "{1..${NUMBER_OF_ITEM}}")
do
    echo "Welcome $i times"
    pubDate=$(date -d "$i day ago" --rfc-2822)
    cat << EOF >> /var/www/podcast/rss.xml
        <item>
            <title>Episode ${i}</title>
            <description>Description Item ${i}</description>
            <pubDate>${pubDate}</pubDate>
            <itunes:explicit>No</itunes:explicit>
            <itunes:subtitle>Description Item ${i}</itunes:subtitle>
            <itunes:summary>Description Item ${i}</itunes:summary>
            <guid>http://localhost:7070/episode_${i}.mp3</guid>
            <itunes:image>http://localhost:7070/fake.jpg</itunes:image>
            <media:thumbnail url="http://localhost:7070/fake.jpg" />
            <enclosure url="http://localhost:7070/episode_${i}.mp3" length="1024" type="audio/mp3" />
        </item>
EOF
    dd if=/dev/zero of=/var/www/podcast/episode_${i}.mp3 bs=1024 count=0 seek=1024
done


cat << EOF >> /var/www/podcast/rss.xml
    </channel>
</rss>
EOF
