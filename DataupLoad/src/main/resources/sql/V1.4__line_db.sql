-----线体顺序
create table public.line_order (
	id serial primary key,
	line_id int not null,
	order_value int not null,
    update_time timestamp not null default current_timestamp,
    create_time timestamp not null default current_timestamp
);

comment on column public.line_order.id is '主键id';
comment on column public.line_order.line_id is '线体ID';
comment on column public.line_order."order_value" is '线体顺序';
comment on column public.line_order.update_time is '更新时间';
comment on column public.line_order.create_time is '创建时间';

create trigger t_line_order before update on public.line_order for each row execute procedure upd_timestamp();