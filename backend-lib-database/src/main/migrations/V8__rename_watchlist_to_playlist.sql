
ALTER TABLE watch_list RENAME CONSTRAINT watch_list_pkey TO playlist_pkey;
ALTER TABLE watch_list RENAME CONSTRAINT watch_list_name_key TO playlist_name_key;
ALTER TABLE watch_list RENAME TO playlist;

ALTER TABLE watch_list_items RENAME COLUMN watch_lists_id TO playlists_id;
ALTER TABLE watch_list_items RENAME CONSTRAINT watch_list_items_pkey TO playlist_items_pkey;
ALTER TABLE watch_list_items RENAME CONSTRAINT watch_list_items_items_id_fkey TO playlist_items_items_id_fkey;
ALTER TABLE watch_list_items RENAME CONSTRAINT watch_list_items_watch_lists_id_fkey TO playlist_items_watch_lists_id_fkey;
ALTER TABLE watch_list_items RENAME TO playlist_items;
