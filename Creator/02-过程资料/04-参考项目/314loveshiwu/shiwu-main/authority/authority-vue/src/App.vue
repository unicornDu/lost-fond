<template>
  <div id="app">


    <router-view/>
  </div>
</template>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;

  : #2c3e50;
}
*{
  margin: 0;
  padding: 0;
}
nav {
  padding: 30px;
}

nav a {
  font-weight: bold;
  : #2c3e50;
}

nav a.router-link-exact-active {
  : #42b983;
}
.el-card__body,.el-main{
  padding: 0px!important;
}
</style>
