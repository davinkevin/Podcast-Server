create extension "uuid-ossp";

ALTER TABLE PLAYLIST
    ADD COLUMN COVER_ID UUID,
    ADD CONSTRAINT PLAYLIST_COVER_ID_FK FOREIGN KEY (COVER_ID) REFERENCES COVER;

with COVER_CREATION as (
    INSERT INTO cover (id, height, url, width)
       values (uuid_generate_v4(), 600, 'https://placehold.co/600x600?text=No+Cover', 600)
       returning id
)
UPDATE PLAYLIST
set COVER_ID = (select id from COVER_CREATION)
where playlist.COVER_ID IS null;

ALTER TABLE PLAYLIST ALTER COLUMN COVER_ID SET NOT NULL;
