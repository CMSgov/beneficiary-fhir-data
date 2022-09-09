# How to Create BFD Insights QuickSight Dashboards

Note: The `prod` version of each analysis is listed here because it is the only environment for
which we do this analysis. To set up for another environment, replace `prod` with the name of your
environment, such as `prod` or `prod-sbx`. Note that for Athena table names, replace any `-` with
`_`, such as `prod_sbx` instead of `prod-sbx`. (Athena doesn't like hyphens in table names).

1. Go to [QuickSight](https://us-east-1.quicksight.aws.amazon.com/). It's an AWS service but for
some reason it uses a different email-based permissions. You may need to request access.
2. Create the dataset by going to Datasets on the left menu and choosing the New Dataset button on
the right.
    - Choose Athena to open the "New Athena data source" modal.
        - Name your data source. Example: `bfd-prod`
        - Athena Workgroup: `bfd`
        - Create Data Source.
    - "Choose Your Table" modal
        - Catalog: `AwsDataCatalog`
        - Database: `bfd-insights-bfd-prod`
        - Table: `bfd_insights_bfd_prod_daily_combined`
        - Select
    - "Finish datset creation" modal
        - Select "Import to SPICE for quicker analysis"
        - Check "Email owners when a refresh fails"
        - Visualize
3. Analysis screen.
    - Create the "Beneficiaries" sheet.
        - Under Visual Types (on the left), select the second icon for KPI. It looks like an up
        arrow and down arrow.
        - Expand "Field Wells" at the top if it isn't already expanded.
        - Drag `benes_queried` from the left to the "Value" field well at the top. It should
        aggregate by "Sum".
        - Click the pencil in the upper right of the sheet to edit.
            - Expand "Title".
            - Edit title. Make it "Count of Beneficiaries Queried" and select "Align Center".
            - Edit subtitle. Make this one "If a beneficiary is queried five times, it counts
            here all five times." Select "Align Center".
            - Close the "Format visual" tab.
        - Right-click on the down-arrow next to "Sheet 1" and Rename the sheet to "Beneficiaries".
    - Create the "Queried per Day" sheet.
        - Click the + next to the first sheet to add a new sheet.
        - Under Visual Types (on the left), select "Vertical Bar Chart", the seventh icon.
        - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `benes_queried` from the left to "Value" field well at the top. It should
            aggregate by "Sum".
            - Drag `day` to the "X Axis" field well at the top.
        - Click the pencil in the upper right of the sheet to edit.
            - Expand "Title".
            - Edit title. Make it "Beneficiaries Queried per Day" and select "Align Center".
            - Edit subtitle. Make this one "If a beneficiary is queried five times, it counts
            here all five times." Select "Align Center".
            - Close the "Format visual" tab.
        - Right-click on the down-arrow next to "Sheet 2" and Rename the sheet to "Queried per
        Day".
    - Create the "Unique Beneficiaries" sheet.
        - Click the + next to the first sheet to add a new sheet.
        - Under Visual Types (on the left), select the second icon for KPI. It looks like an up
        arrow and down arrow.
        - Expand "Field Wells" at the top if it isn't already expanded.
        - Drag `benes_first_seen` from the left to the "Value" field well at the top. It should
        aggregate by "Sum".
        - Click the pencil in the upper right of the sheet to edit.
            - Expand "Title".
            - Edit title. Make it "Count of Unique Beneficiaries" and select "Align Center".
            - Edit subtitle. Make this one "If a beneficiary is queried five times, it counts
            here only once." Select "Align Center".
            - Close the "Format visual" tab.
        - Right-click on the down-arrow next to "Sheet 3" and Rename the sheet to "Unique
        Beneficiaries".
    - Create the "First Seen per Day" sheet.
        - Click the + next to the first sheet to add a new sheet.
        - Under Visual Types (on the left), select "Vertical Bar Chart", the seventh icon.
        - Expand "Field Wells" at the top if it isn't already expanded.
            - Drag `benes_first_seen` from the left to "Value" field well at the top. It should
            aggregate by "Sum".
            - Drag `day` to the "X Axis" field well at the top.
        - Click the pencil in the upper right of the sheet to edit.
            - Expand "Title".
            - Edit title. Make it "First Seen per Day" and select "Align Center".
            - Edit subtitle. Make this one "If a beneficiary is queried five times, it counts
            here only once and only on the day first queried." Select "Align Center".
            - Close the "Format visual" tab.
        - Right-click on the down-arrow next to "Sheet 4" and Rename the sheet to "First Seen per
        Day".
4. Create the dashboard.
    - While still on the analysis screen, in the upper-right, click Share > Publish Dashboard.
    Make it "BFD Beneficiaries (Prod)".
    - The default options should otherwise be fine, so click Publish Dashboard.
5. Make the dashboard public.
    - While still on the dashboard screen, in the upper right, click Share > Share dashboard.
    - On the left, there is a toggle under "Enable access for" labeled "Everyone in this
    account". Turn it on.
    - On the left, there is also a toggle labeled "Discoverable in QuickSight". Turn that one on
    also.
6. Set the SPICE refresh.
    - Return to the main QuickSight index.
    - Select "Datasets" on the left.
    - Click on the dataset you created in step 2 above.
    - Click on the Refresh tab.
    - In the upper right, click on "Add New Schedule".
        - The defaults should be what you want.
            - "Full Refresh"
            - Timezone should be "America/New_York"
            - Start time should be "<date> 11:59 PM"
            - Frequency "Daily"
        - Save.
