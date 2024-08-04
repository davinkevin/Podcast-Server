create extension "uuid-ossp";

alter table playlist
    add column cover_id uuid,
    add constraint playlist_cover_id_fk foreign key (cover_id) references cover;

with cover_creation as (
    insert into cover (id, height, url, width)
        values (uuid_generate_v4(), 600, 'https://placehold.co/600x600?text=no+cover', 600)
        returning id
)
update playlist
set cover_id = (select id from cover_creation)
where playlist.cover_id is null;

alter table playlist alter column cover_id set not null;
