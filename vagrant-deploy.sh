PATH_OF_CURRENT_DIRECTORY="$(pwd)"

#source $PATH_OF_CURRENT_DIRECTORY/vagrant/vagrant_functions.sh


IMIS_INTEGRATION_HOME="/bahmni/insurance-connect"

echo "$IMIS_INTEGRATION_HOME"

cd "$IMIS_INTEGRATION_HOME/target/"
echo "$IMIS_INTEGRATION_HOME/target/"
mv -f insurance-integration-0.0.1-SNAPSHOT.jar  insurance-integration.jar

cd "$BAHMNI_HOME/"

sudo cp -f $IMIS_INTEGRATION_HOME/target/insurance-integration.jar /opt/insurance-integration/bin/
sudo service insurance-integration restart
