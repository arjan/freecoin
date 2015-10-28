# Building the staging environment

This folder contains tools enabling an automagic deployment of the staging environment in a vagrant container.


## Prerequisities

You'll need the following tools installet in your $PATH. The quoted versions (or newer) are recommended
* virtualbox 5.0
* vagrant 1.7.4
* ansible 2.0.0


For deployment, configuration can be provided via environment variables:
```
TWITTER_CONSUMER_TOKEN="YOUR_CONSUMER_TOKEN_FROM_TWITTER"\
TWITTER_SECRET_TOKEN="YOUR_SECRET_TOKEN_FROM_TWITTER"\
java -jar freecoin.jar
```

## INSTALL

The from the ops/ directory simply run:

```
$ vagrant up staging_vm
```

After the build is complete, log in and change to the staging user:

```
$ vagrant ssh
su - staging
```

The cloned repository can be found in ./freecoin. This install als builds the jar, which can be found in the usual location, ./freecoin/target

## Rebuild from scratch
In case something goes terribly wrong or you simply want to rebuild the whole machine, run the following commands:

```
$ vagrant destroy
$ vagrant up staging_vm
```
