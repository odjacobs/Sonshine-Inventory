CREATE TABLE categories
(
    id            INT          NOT NULL,
    name          VARCHAR(255) NOT NULL,
    active        BIT(1)       NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE items
(
    id          INT          NOT NULL,
    category_id INT          NOT NULL,
    name        VARCHAR(255) NOT NULL,
    unit_label  VARCHAR(255) NULL,
    quantity    INT          NOT NULL,
    quota       INT NULL,
    active      BIT(1)       NOT NULL,
    CONSTRAINT pk_items PRIMARY KEY (id)
);

CREATE TABLE pledges
(
    id            INT          NOT NULL,
    item_id       INT          NOT NULL,
    donor_name    VARCHAR(255) NOT NULL,
    donor_contact VARCHAR(255) NOT NULL,
    quantity      INT          NOT NULL,
    status        VARCHAR(255) NULL,
    created_at    datetime     NOT NULL,
    expires_at    datetime     NOT NULL,
    fulfilled_at  datetime NULL,
    CONSTRAINT pk_pledges PRIMARY KEY (id)
);