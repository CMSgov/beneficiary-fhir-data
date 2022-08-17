# Global Pipeline Libraries

The _groovy_ included in this directory hierarchy is for use with [Jenkins Global Pipeline Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/).

## Defining Global Pipeline Libraries

Global Pipeline Libraries can be defined under the _Global Pipeline Libraries_ section via the Jenkins Configuration UI (_Jenkins Project_ e.g. `bfd` >> _Manage Jenkins_ >> Configure System ).

When _.groovy_ files are included in the `./vars` sub directory, they are exposed to Jenkins by the name of the file **without** its extension.
The methods defined within can be called as you might call a class method of the same name.
As of this writing, there are a handful of AWS-specific methods defined, the most common of which is the role assumption method (`assumeRole`) stored under [awsAuth.groovy](./vars/awsAuth.groovy). It can be called like so: `awsAuth.assumeRole()`.

## Usage and Development

> NOTE: As of this writing, implicit loading of the library is enabled, making the global bfd libraries available without the need for explicit calls to @Library as below.

This directory is defined as the `bfd` Global Pipeline Library. Methods defined in the files stored under the `./vars` directory can be called like so:

``` groovy
// Explicit `@Library` call is required when implicit loading is not configured or users wish to override the default version
// See the Development section below for more information.
@Library('bfd') _ 
awsAuth.assumeRole()
```

If you want to introduced changes and further develop the global libraries here, you will be making your changes against a non-default, non-trunk branch.
Remember to take care to explicitly target your branch by introducing the line `@Library('bfd@your-branch-here')` to your solution.
See the [Library version documentation](https://www.jenkins.io/doc/book/pipeline/shared-libraries/#library-versions) for more details.
