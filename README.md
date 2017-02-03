# PGP Vault

[![Join the chat at https://gitter.im/pgpvault/Lobby](https://badges.gitter.im/pgpvault/Lobby.svg)](https://gitter.im/pgpvault/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
Desktop GUI application for easy and regular PGP decryption/encryption of specific files on a file system.

I tend to store sensitive information on my computer and sometimes sync this to google drive, drop box, etc.. But I don't want this information to be stored in unsecured state. I want it to be encrypted. 

PGP software which is already exists doesn't seem to support this use case in a user-friendly way. It requires me to perform couple manual operations before I can get my file decrypted and then it requires pretty much same amount of manual operations to encrypt it back. 

At the end of the day what I need is an application which will require minimum number of user actions to make following workflow possible: 

1. Double click on encrypted file
2. File is decrypted by PGPVault and stored in Temp (configurable) folder
3. Respective application (registered in OS to handle files with this extension) opened
4. PGPVault offers to encrypt it back (and delete temp file after that)

It's that simple. 

# Efforts source
I'm hoping to use crowd funding for open-source projects in order to support this project as I don't have enough personal time to complete fully-featured version in a reasonable timeframe. 
I'm going to place initial source code and invite others to participate in this project.

# Road Map (versions)
1. Minimum Viable Product. Only primary use cases are implemented. Not ready for regular consumers (non-technical people)
2. Add certificate creation and export, program installer, auto-update
3. Singatures support, Batch operations

We'll see what happens next once we get to this point...

# Current state
Current state can be seen here: https://github.com/skarpushin/pgpvault/milestones

# Documentation
Once ready documentation will be placed to WiKi: https://github.com/skarpushin/pgpvault/wiki

# Come join this project!
We're happy to receive help from anybody! Just comment on task that you started work on it and then make a pull request once completed.

It's open source. There are couple rules though that we ask you to follow, please read requirements documents carefully.
