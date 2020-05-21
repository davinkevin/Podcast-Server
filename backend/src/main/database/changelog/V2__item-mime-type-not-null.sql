UPDATE item SET mime_type='video/webm'        where (mime_type is null OR mime_type='') and url ~* '\.youtube.com/watch.*';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and url ~* '\.dailymotion.com.*';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and url ~* '\.tf1.fr.*';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and url ~* '\.francetv\.fr.*';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and url ~* '\.france\.tv.*';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.m3u8$';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.mp4$';
UPDATE item SET mime_type='audio/mp3'         where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.mp3$';
UPDATE item SET mime_type='audio/m4a'         where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.m4a$';
UPDATE item SET mime_type='video/mp4'         where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.m4v$';
UPDATE item SET mime_type='video/quicktime'   where (mime_type is null OR mime_type='') and split_part(url, '?', 1) ~* '\.mov$';

ALTER TABLE ITEM ALTER COLUMN MIME_TYPE SET NOT NULL;
ALTER TABLE ITEM ADD CONSTRAINT ITEM_MIME_TYPE_NOT_EMPTY CHECK (MIME_TYPE <> '' AND MIME_TYPE LIKE '%/%');
