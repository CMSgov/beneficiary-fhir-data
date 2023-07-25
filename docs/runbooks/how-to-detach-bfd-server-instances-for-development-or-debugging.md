# How to Detach BFD Server Instances for Development or Debugging

Follow this runbook to safely detach a BFD server instance for development or debugging using the AWS Console. It is assumed you have already signed in to the AWS Console while following the runbook.

- Detach a BFD Server Instance for Development or Debugging
  - [Scale Out to Avoid Potential Performance Degradation](#scale-out-to-avoid-potential-performance-degradation)
  - [Detach the Instance](#detach-the-instance)
  - [Scale In After Detaching an Instance](#scale-in-after-detaching-an-instance)
- [Patching Regularly When Instances Endure Beyond ~1 Day](#patching-regularly-when-instances-endure-beyond-1-day)
- [Terminating the Instance](#terminating-the-instance)

## Scale Out to Avoid Potential Performance Degradation

  1. Navigate to the **Auto Scaling Groups** page, it is located under the **EC2** service.
  1. Click on the listed group corresponding to the desired environment.
    1. i.e. if `TEST` is the desired environment, click on `bfd-test-fhir...`
  1. On the **Details** tab, choose **Group details, Edit**.
  1. Increment the desired capacity by one. For example, if the current value is 6, enter 7.  
    1. The desired capacity must be less than or equal to the maximum size of the group. If your new value for **Desired capacity** is greater than **Maximum capacity**, you must update **Maximum capacity**.
  1. When you are finished, choose **Update**.

Once you've verified that your **Auto Scaling** group has launched one additional instance, you are ready to [detach the instance](#detach-the-instance).

## Detach the Instance

NOTE: Before detaching an instance, ensure you have already [scaled out the Auto Scaling Group to avoid potential performance degradation](#scale-out-to-avoid-potential-performance-degradation).

1. Navigate to the **Auto Scaling Groups** page, it is located under the **EC2** service.
1. Click on the listed group corresponding to the desired environment.
      1. i.e. if `TEST` is the desired environment, click on `bfd-test-fhir...`
1. On the **Instance management** tab, in **Instances**, select the checkbox for the instance and choose **Actions, Detach**.
1. In the **Detach instance** dialog box, leave the checkbox to launch a replacement instance unchecked. Choose **Detach instance**.

Once you've verified that your instance has successfully detached, you are ready to rename the instance.

1. In the **Auto Scaling Group** list, the detached instance will still exist in the group; note the ID of the instance.
1. Go to **Services > EC2** and click **Instances**
1. Find the detached instance by its **ID** in the list
1. Click the **Edit** button near the **ID** of the detached instance and rename it "`[ORIGINAL INSTANCE NAME]`-`[FIRST INITIAL][LAST NAME]`". E.g., `bfd-test-fhir-jsmith` if the original name is `bfd-test-fhir` and the user's full name is `John Smith`.

Once you've verified that your instance has been renamed, you are ready to [scale back in its Auto Scaling Group](#scale-in-after-detaching-an-instance).

## Scale In After Detaching an Instance

NOTE: This should almost never be done without having first recently [scaled out the Auto Scaling Group](#scale-out-to-avoid-potential-performance-degradation).

1. Navigate to the **Auto Scaling Groups** page, it is located under the **EC2** service.
1. Click on the listed group corresponding to the desired environment.
    1. i.e. if `TEST` is the desired environment, click on `bfd-test-fhir...`
1. On the **Details** tab, choose **Group details, Edit**.
1. Decrement the desired capacity by one. For example, if the current value is 7, enter 6.
1. When you are finished, choose **Update**.

## Patching Regularly When Instances Endure Beyond ~1 Day

Detached instances should be [terminated](#terminating-the-instance) as soon as possible, however, for those instances living beyond ~1 day, we should be mindful of any updates which may need to be applied. 

Some examples:

1. Performing `yum update` on the detached server instance.
1. Noting any updates to the AMI the server instance is built from, and applying those updates to the instance as needed.

## Terminating the Instance

1. Navigate to the **EC2** services page.
1. Click on **Instances (running)** and select the instance.
1. In the details page which opens, click the **Instance state** dropdown and select **Terminate instance**.
1. Choose **Terminate** when prompted for confirmation.
