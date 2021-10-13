<template>
  <div class="showGDSRecord">
    <img src="./../assets/spring-boot-vuejs-logo.png">
    <h1>{{ hellomsg }}</h1>
    <h2>List of cities</h2>
    <table>
            <tr>
                <th>Id</th>
                <th>Name</th>
                <th>Population</th>
            </tr>
            
            <tr th:each="city : ${cities}">
                <td th:text="${city.id}">Id</td>
                <td th:text="${city.name}">Name</td>
                <td th:text="${city.population}">Population</td>
            </tr>
        </table>
  </div>
</template>

<script>
export default {
  name: 'showGDSRecords',
  props: { hellomsg: { type: String, required: true } }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}
</style>