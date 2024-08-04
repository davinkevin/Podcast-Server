alter table watch_list rename constraint watch_list_pkey to playlist_pkey;
alter table watch_list rename constraint watch_list_name_key to playlist_name_key;
alter table watch_list rename to playlist;

alter table watch_list_items rename column watch_lists_id to playlists_id;
alter table watch_list_items rename constraint watch_list_items_pkey to playlist_items_pkey;
alter table watch_list_items rename constraint watch_list_items_items_id_fkey to playlist_items_items_id_fkey;
alter table watch_list_items rename constraint watch_list_items_watch_lists_id_fkey to playlist_items_playlists_id_fkey;
alter table watch_list_items rename to playlist_items;
