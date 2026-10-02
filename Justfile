bootstrap:
    ./hooks/install-java-format.sh
    brew install yq taplo prek tenv
    # We overwrite the default prek hook with our own script
    # that will automatically re-add any formatting changes before committing
    # This is not possible out of the box at the time of writing this
    # See https://github.com/j178/prek/issues/1051
    prek install --force
    mv .git/hooks/pre-commit .git/hooks/pre-commit.prek
    cp -p ./hooks/run-pre-commit.sh .git/hooks/pre-commit
    cp -p ./hooks/rerun-changed.sh .git/hooks/pre-commit.rerun
