# Deployment Notes
To deploy, you need to be logged on npm.
```shell
npm login
```

After logged, you need to push a new versión
If the sdk has bugfixes:
```shell
npm version patch
```
Or if it has great behavior changes:
```shell
npm version minor
```
And if there is a complete change in it
```shell
npm version major
```

To deploy and publish on npm:
```shell
npm publish --access public
```

You can verify that the version is public here

[NPM SDK Node](https://www.npmjs.com/package/@slingr/slingr-services)