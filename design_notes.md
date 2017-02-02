# Design Notes

## Initial Thoughts on Architecture

1. Branches suck:
    * DNS names can be built based on branch names.
    * No clue what to do with Maven artifacts and their dependencies, though.
    * Even without branches, I don't think Maven's "LATEST" version range is working with the current setup.
        * Maybe I can pass the artifact versions into the script, and only have it run the portions of the script that it has versions for?
1. This is a concurrent system.
    * Needs a global lock: only one deployment running per branch, ever, across all jobs.
    * I think Jenkins' workflow system provides this kind of lock.
1. Green/blue deployments are a must.
    * Need a configurable proxy to roll over incoming HTTP requests: nginx or AWS gateway
    * Can't roll over ETL, though:
        * Have to ensure only one ETL service ever running per branch:
            1. Ensure that the FHIR servers have ben updated and cut over successfully before touching the ETL server.
            1. If it exists and is running, stop the ETL service.
            1. Deploy the ETL server.
            1. Start the ETL server.
            1. Push the sample data back into S3.
            1. Wait for that sample data set to be processed successfully.
1. Going to need an artifact repo that's more current than OSSRH; can't wait 3h for things to appear.
    * Simplest solution: local Maven repository.
1. One project that handles entire sandbox (FHIR and ETL). Gets fired by other jobs as needed.
1. Should probably have 3 FHIR servers: one write site, 2 load-balanced read sites.
    * Can make the read sites part of an auto-scaling group later.
    * Hmmm... can the write site be auto-scaled as well? If so, no need for separation of responsibility...
        * Yes, I think it can.
1. I guess I can't ever see a reason to deploy branched builds to the sandbox.
    * Still doesn't solve the "LATEST" issue, though.
1. How do I ensure that the ETL gets sample data pushed through it?
    * Maybe a `load_etl.yml` include that's guarded by a `when` clause that checks to see if the FHIR server is currently empty?
    * Or maybe I just do this manually. Once. The end.
1. Will I have any trouble with SSL key management for the FHIR servers?
    * Not for the clients (i.e. trust store): I can easily automate adding their public keys.
    * I think the servers (i.e. the keystore) have to use a wildcard cert, though.
    * Is it even possible to generate a self-signed wildcard cert?
        * Seems that the answer is "yes," though keytool may not allow it itself (might have to use openssl, instead).
1. In general, what's the best way to handle redoing the initial load?
    * If we stand up a new DB, we will temporarily need double the storage.
    * If we re-use an existing DB, unless versions are erased, we end up permanently needing (roughly) double the storage.
        * I kind of like the idea of this: we end up with a history of how the EOBs looked and were structured over time.
    * Will running into a non-clean DB risk out-of-order problems with updates that were generated before the re-load data sets?
        * The workaround for this is to insist that anytime a reload is generated, all pending update data sets must be either fully processed or dropped first.
        * But if we ever start allowing `DELETE` operations, we'll have to start with a clean DB. Much harder to avoid issues with those.
    * Need to ensure that INSERT ops don't cause errors/dupes: either always upsert, or add a `type { INITIAL_LOAD | UPDATE }` field to the data set manifests.
    * I'm pretty tempted to say that we need to ensure re-loads only push resources that have actually changed.
        * That's a fairly major architectural change, though.

## SSL Handling

Assuming load-balanced FHIR servers, how do I handle SSL server and client certificates correctly and securely?

* If each EC2 instance is creating its own server keys, I _could_ configure the ETL server to pull the public certs for those from S3 (or something like that), and get it working. It'd be a bit rickety, but it'd work.
* However, I don't see any way for the frontend servers to securely and reliably handle that: how will they know when new public certs are needed?
* I'm going to have to use a single shared wildcard cert for the FHIR server. The other certs are all client certs so don't need to be wildcards, but should still be shared to simplify orchestration.
* I need some sort of secure key management system: where will the private keys be kept once they're generated?
    * S3, encrypted.

## Encryption

Well, I just fell down a rabbit hole... If I'm going to create AMIs with private keys in them, those AMIs need to be encrypted. In order to encrypt AMIs, I need to use AWS' Key Management Service (KMS).

* The base AMI will have to be encrypted **before** I install and configure the FHIR server (i.e. before the private key is written to the instance).
* This article gives a solid starting point for how to create an encrypted AMI: [Encrypted Amazon EC2 boot volumes with Packer and Ansible](http://www.davekonopka.com/2016/ec2-encrypted-boot-volumes.html).

