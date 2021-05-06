CREATE OR REPLACE FUNCTION track_bene_monthly_change() RETURNS TRIGGER
AS $beneficiary_monthly_audit$
    BEGIN
        --
        -- Create a row in beneficiary_monthly_audit to reflect the operation performed
        -- on BeneficiaryMonthly; variable TG_OP denotes the operation.
        --
        IF (TG_OP = 'UPDATE') THEN
                INSERT INTO public.beneficiary_monthly_audit VALUES (OLD.*, 'U', now());
            --END IF;
            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
            INSERT INTO public.beneficiary_monthly_audit VALUES(NEW.*, 'I', now());
            RETURN NEW;
        ELSIF (TG_OP = 'DELETE') THEN
            INSERT INTO public.beneficiary_monthly_audit VALUES(OLD.*, 'D', now());
            RETURN NEW;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END;
$beneficiary_monthly_audit$ LANGUAGE plpgsql;
