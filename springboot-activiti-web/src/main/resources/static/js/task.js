var app = new Vue({
    el: "#app",
    data: {
        tasklist: []
    },
    created: function () {
        //调用方法
        this.findAll();
    },
    methods: {
        findAll: function () {
            axios.get('/flow/tb_site_messageInfo')
                .then(function (result) {
                    console.log(result);
                    app.tasklist = result.data;
                });
            ;
        },
        doTask: function (taskId) {
            axios.put('/flow/completeTask/' + taskId)
                .then(function (result) {
                    console.log(result);
                    app.tasklist = result.data;
                });
        }
    }
});