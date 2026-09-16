bootstrap:
    ./hooks/install-java-format.sh
    brew install yq taplo tofu prek
    prek install --force
    mv .git/hooks/pre-commit .git/hooks/pre-commit.prek
    cp -p ./hooks/run-pre-commit.sh .git/hooks/pre-commit
    cp -p ./hooks/rerun-changed.sh .git/hooks/pre-commit.rerun
