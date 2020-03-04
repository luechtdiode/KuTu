-- -----------------------------------------------------
-- Migrate default-values for each competition
-- -----------------------------------------------------
INSERT INTO wettkampf_plan_zeiten (wettkampfdisziplin_id, wettkampf_id, wechsel, einturnen, uebung, wertung)
    SELECT
        wkd.id as wettkampfdisziplin_id,
        wk.id as wettkampf_id,
        30000 as wechsel,
        30000 as eintrunen,
        case
            when pd.name in ('K1', 'K2', 'K3', 'K4') then 40000
            when pd.name like 'P%' then 60000
            else 50000
        end as uebung,
        case
            when pd.name in ('K1', 'K2', 'K3', 'K4') then 40000
            when pd.name like 'P%' then 60000
            else 50000
        end as wertung
    FROM
        wettkampf wk
        inner join programm p on (wk.programm_id = p.id)
        inner join programm pd on (p.id = pd.parent_id)
        inner join wettkampfdisziplin wkd on (pd.id = wkd.programm_id)
        inner join disziplin d on (d.id = wkd.disziplin_id)
;
