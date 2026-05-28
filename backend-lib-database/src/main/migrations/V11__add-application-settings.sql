create table application_settings (
    id                          uuid                     not null default uuidv7(),
    created_at                  timestamp with time zone not null default now(),
    concurrent_download         integer                  not null default 3,
    number_of_try               integer                  not null default 10,
    number_of_day_to_download   bigint                   not null default 30,
    number_of_day_to_save_cover bigint                   not null default 365,

    primary key (id)
);

insert into application_settings default values;
