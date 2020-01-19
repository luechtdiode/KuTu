-- -----------------------------------------------------
-- Migrate default-values for each competition
-- -----------------------------------------------------
INSERT INTO durchgang (wettkampf_id, ordinal, name, title, durchgangType, planStartOffset, effectiveStartTime, effectiveEndTime)
    SELECT
        zp.wettkampf_id,
        coalesce((SELECT max(ordinal)+1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id), 0) as ordinal,
        zp.durchgang as name,
        zp.durchgang as title,
        1 as durchgangType,
        0 as planStartOffset,
        null as effectiveStartTime,
        null as effectiveEndTime
    FROM
        zeitplan zp
    WHERE
        NOT EXISTS (SELECT 1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id and dd.name = zp.durchgang)
;
