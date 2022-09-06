# How to Create BFD Insights QuickSight Dashboards

Note: Replace `<environment>` with the name of your environment, such as `prod` or `prod-sbx`.
Replace any `-` with `_` in `<underscore_environment>` (Athena doesn't like hyphens in table
names).

1. Go to [QuickSight](https://us-east-1.quicksight.aws.amazon.com/). It's an AWS service but for some reason it uses a different email-based permissions. You may need to request access.
2. Create the Unique Beneficiaries dashboard.
    - Create the dataset.
        - Datasets. New Dataset.
        - Athena.
            - Name your data source. Example: `bfd-<environment>-unique-beneficiaries`
            - Athena Workgroup: `bfd`
            - Create Data Source.
        - Choose Your Table.
            - Catalog: `AwsDataCatalog`
            - Database: `bfd-<environment>`
            - Table: Choose the one you want to query. Ex:
            `bfd_<underscore_environment>_beneficiaries`
            - Use Custom SQL.
        - Enter custom SQL query
            - At the top, where is says "New custom SQL", replace that with the title of the data
            source, such as `bfd-<environment>-beneficiaries`.
            - Enter this:
            `SELECT bene_id, MIN(timestamp) AS first_seen FROM bfd-insights-bfd-<environment>"."bfd_insights_bfd_<underscore_environment>_api_requests_beneficiaries" GROUP BY "bene_id"`
            - Confirm query.
        - Finish dataset creation.
            - Directly query your data.
            - Visualize.
    - Create the analysis.
        - Add a Key Performance Indicator (KPI).
            - Under Visual Types (on the left), select "KPI" (it's second in the list and looks
            like two arrows over a line)
            - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `bene_id` from the left to the "Value" field well at the top.
            - Click the pencil in the upper right of the sheet to edit.
                - Expand "Title".
                - Edit title. Make it "Count of unique beneficiaries queried".
                - Edit subtitle. Make this one "If a beneficiary is queried five times, it counts
                here only once."
            - Right-click on the down-arrow next to "Sheet 1" and Rename the sheet to "Unique
            Beneficiaries".
        - Add a Vertical Bar Chart sheet.
            - Click the + next to the first sheet to add a new sheet.
            - Under Visual Types (on the left), select "Vertical Bar Chart".
            - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `bene_id` from the left to "Value" field well at the top.
            - Drag `first_seen` to the "X Axis" field well at the top.
            - Right-click on the down-arrow next to "Sheet 2" and Rename the sheet to "First
            Queried".
    - Create the dashboard.
        - While still on the analysis screen, in the upper-right, click Share > Publish Dashboard.
        Choose a name. Example: `<environment> Unique Beneficiaries`.
        - The default options should otherwise be fine, so click Publish Dashboard.
    - Make the dashboard public.
        - While still on the dashboard screen, in the upper right, click Share > Share dashboard.
        - On the left, there is a toggle under "Enable access for" labeled "Everyone in this
        account". Turn it on.
2. Create the Beneficiaries dashboard.
    - Create the dataset.
        - Datasets. New Dataset.
        - Athena.
            - Name your data source. Example: `bfd-<environment>-beneficiaries`
            - Athena Workgroup: `bfd`
            - Create Data Source.
        - Choose Your Table.
            - Catalog: `AwsDataCatalog`
            - Database: `bfd-<environment>`
            - Table: Choose the one you want to query. Ex:
            `bfd_<underscore_environment>_beneficiaries`
            - Select.
        - Finish dataset creation.
            - Directly query your data.
            - Visualize.
        - Finish dataset creation.
            - Directly query your data.
            - Visualize.
    - Create the analysis.
        - Add a Key Performance Indicator (KPI).
            - Under Visual Types (on the left), select "KPI" (it's second in the list and looks
            like two arrows over a line)
            - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `bene_id` from the left to the "Value" field well at the top.
            - Click the pencil in the upper right of the sheet to edit.
                - Expand "Title".
                - Edit title. Make it "Count of beneficiaries queried".
                - Edit subtitle. Make this one "Not unique. If a beneficiary is queried five times,
                it counts here all five times."
            - Right-click on the down-arrow next to "Sheet 1" and Rename the sheet to "Beneficiary
            Queries".
        - Add a Vertical Bar Chart sheet.
            - Click the + next to the first sheet to add a new sheet.
            - Under Visual Types (on the left), select "Vertical Bar Chart".
            - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `bene_id` from the left to "Value" field well at the top.
            - Drag `timestamp` to the "X Axis" field well at the top.
            - Right-click on the down-arrow next to "Sheet 2" and Rename the sheet to "Daily
            Queries".
    - Create the dashboard.
        - While still on the analysis screen, in the upper-right, click Share > Publish Dashboard.
        Choose a name. Example: `<environment> Beneficiaries`.
        - The default options should otherwise be fine, so click Publish Dashboard.
    - Make the dashboard public.
        - While still on the dashboard screen, in the upper right, click Share > Share dashboard.
        - On the left, there is a toggle under "Enable access for" labeled "Everyone in this
        account". Turn it on.