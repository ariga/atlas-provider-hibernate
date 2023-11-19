create table Event (id bigint not null, title varchar(255), primary key (id)) engine=InnoDB;
create table Event_SEQ (next_val bigint) engine=InnoDB;
insert into Event_SEQ values ( 1 );
create table Location (id bigint not null auto_increment, primary key (id)) engine=InnoDB;
