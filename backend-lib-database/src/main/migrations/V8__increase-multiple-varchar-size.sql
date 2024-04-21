alter table item
    alter column file_name type varchar(65535) using file_name::varchar(65535),
    alter column title type varchar(65535) using title::varchar(65535);

alter table cover
    alter column url type varchar(65535) using url::varchar(65535);

alter table podcast
    alter column title type varchar(65535) using title::varchar(65535);