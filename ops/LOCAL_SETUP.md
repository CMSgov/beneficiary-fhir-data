# Ops Local setup Setup

## Recommended .kion.yml

Below is a recommended setup for you kion.yml. This creates short cuts to connect to the various environments.

### File Contents

```yaml
---
kion:
  url: https://cloudtamer.cms.gov
  username: <your EUA id here>
  idms_id: 2

browser:
  firefox_containers: true

favorites:
  - name: legacy
    account: <account_number>
    region: us-east-1
    cloud_access_role: BFD Application Admin
    browser: firefox

  - name: n
    account: <account_number>
    region: us-east-1
    cloud_access_role: BFD Application Admin
    browser: firefox

  - name: wn
    account: <account_number>
    region: us-east-1
    cloud_access_role: BFD Application Admin
    access_type: web
    browser: firefox

  - name: nd
    account: <account_number>
    region: us-west-2
    cloud_access_role: BFD Application Admin
    browser: firefox

  - name: p
    account: <account_number>
    region: us-east-1
    cloud_access_role: BFD Application Admin
    browser: firefox

  - name: wp
    account: <account_number>
    region: us-east-1
    cloud_access_role: BFD Application Admin
    access_type: web
    browser: firefox

  - name: pd
    account: <account_number>
    region: us-west-2
    cloud_access_role: BFD Application Admin
    browser: firefox

# <any additional favorites below> 
```
### Details
The naming pattern is as follows
- legacy is our Legacy AWS account
- n  *n*on-prod
- wn *w*eb-browser *n*on-prod
- nd  *n*on-prod *d*isaster recovery



## AWS Config

Put the following in your ~/.aws/config file. This is required for Kion to work properly with the AWS CLI.

```aws
[default]
region=us-east-1

[profile legacy]
credential_process = /opt/homebrew/bin/kion f --credential-process legacy
region=us-east-1

[profile non-prod]
credential_process = /opt/homebrew/bin/kion f --credential-process n
region=us-east-1
cli_pager=

[profile non-prod-dr]
credential_process = /opt/homebrew/bin/kion f --credential-process n
region=us-west-2
cli_pager=

[profile prod]
credential_process = /opt/homebrew/bin/kion f --credential-process p
region=us-east-1
cli_pager=

[profile prod-dr]
credential_process = /opt/homebrew/bin/kion f --credential-process p
region=us-west-2
cli_pager=
```

## AWS CLI and Session Manager Plugin

If you see this error while using AWS CLI commands that rely on SSM sessions:

```text
aws: [ERROR]: SessionManagerPlugin is not found
```

Install and update AWS tooling with Homebrew:

```bash
# Install or update AWS CLI v2
brew install awscli || brew upgrade awscli

# Install Session Manager plugin (required for SSM Session Manager)
brew install --cask session-manager-plugin
```

Verify both are available:

```bash
which aws && aws --version
which session-manager-plugin
```

If `which session-manager-plugin` does not return a path, ensure this directory is on your PATH:

```bash
/usr/local/sessionmanagerplugin/bin
```

## Recommended Aliases

Add to .bashrc or .zshrc for a convenient way to switch between AWS profiles and open Kion in the browser.

```bash
alias k=kion
alias kl="export AWS_PROFILE=\"legacy\" && echo 'Switched to legacy'"
alias kwl="k f --web legacy"
alias kn="export AWS_PROFILE=\"non-prod\" && echo 'Switched to non-prod'"
alias kwn="k f wn"
alias kp="export AWS_PROFILE=\"prod\" && echo 'Switched to prod'"
alias kwp="k f wp"
```

## tofu Helpers

The following functions and aliases are useful for working with the tofu CLI. Add to .bashrc or .zshrc. These assume you have installed the tofu CLI and have it in your PATH. If you are using tenv, you may need to adjust the path to the tofu binary. 
```tenv (brew install tenv)```

```bash
# Tofu helpers

tenv_tofu() {
  "$(tenv update-path | cut -f 1 -d ':')"/tofu "$@"
}

tofu_workspace() {
  local wksp="$1"
  local is_platform="$([[ $(pwd | rg 'ops/platform') ]] && echo "true" || echo "false")"
  if rg "(test|non-prod)$" &>/dev/null <<<"$wksp"; then
    export AWS_PROFILE=non-prod
  else
    export AWS_PROFILE=prod
  fi

  if [[ $is_platform == "true" ]]; then
    TF_WORKSPACE=default tenv_tofu init -var account_type="$wksp" -reconfigure && tenv_tofu workspace select -var account_type="$wksp" -or-create "$wksp"
  else
    local parent_env="$(rg -o "(test|prod|sandbox)$" --replace '$1' <<<"$wksp")"
    TF_WORKSPACE=default tenv_tofu init -var parent_env="$parent_env" -reconfigure && tenv_tofu workspace select -var parent_env="$parent_env" -or-create "$wksp" 
  fi
}

alias tfw="tofu_workspace"

tf() {
  local wksp="$(cat ./.terraform/environment 2>/dev/null | head -n1 || echo "none")"
  if rg "(test|non-prod)$" &>/dev/null <<<"$wksp"; then
    export AWS_PROFILE=non-prod
  elif rg "(sandbox|prod)$" &>/dev/null <<<"$wksp"; then
    export AWS_PROFILE=prod
  else
    echo "No workspace selected; use tfw <environment> to select a workspace"
    return 1
  fi

  tenv_tofu "$@"
}

alias tofu="tf"
alias terraform="tf"
```