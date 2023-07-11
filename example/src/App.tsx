import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import Beacon from 'react-native-beacon';

Beacon.setOptions({
  android: {
    notification: {
      contentTitle: 'Beacon scan',
      contentText: 'Beacon scan started',
    },
  },
});

Beacon.watchBeacons((beacons) => {
  console.log(beacons);
});

export default function App() {
  const [scanStarted, setScanStarted] = React.useState(false);

  React.useEffect(() => {
    Beacon.requestPermissions();
  }, []);

  const toggleScan = () => {
    if (scanStarted) {
      Beacon.stopBeaconScan().then(() => {
        setScanStarted(false);
      });
    } else {
      Beacon.startBeaconScan([
        {
          id: 'my-beacon',
          uuid: 'D9FF627C-FE7D-425C-9CE8-A0652E6738FF',
        },
      ]).then(() => {
        setScanStarted(true);
      });
    }
  };

  return (
    <View style={styles.container}>
      <Text>Hello, World!</Text>
      <Button title="Enable BT" onPress={Beacon.enableBluetooth} />
      <Button
        title={scanStarted ? 'Stop scan' : 'Start scan'}
        onPress={toggleScan}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
