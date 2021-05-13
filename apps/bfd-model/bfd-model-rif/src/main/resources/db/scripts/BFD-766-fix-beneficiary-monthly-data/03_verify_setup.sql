--
-- Verify that the triggers were created
--
select
	trigger_name, string_agg(event_manipulation, ',') as event, action_timing as activation
from information_schema.triggers
group by 1, 3;