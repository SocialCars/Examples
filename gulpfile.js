var paths = {

    assets: [],

    sass: [],

    less: [],

    css: [
        "node_modules/bootstrap/dist/css/bootstrap*(|-theme).css"
    ],

    js: "",

    js_watch: [
        "node_modules/jquery/dist/jquery.js",
        "node_modules/bootstrap/dist/js/bootstrap.js"
    ],

    partials: [],

    js_lint: [],

    js_cover: [],

    mocha: [],

    fonts: "",

    build: "target/js",

    images: "",

    admin: {
        assets: [
            "node_modules/bootstrap/dist/*fonts/*",
            "node_modules/font-awesome/*fonts/*",
            "node_modules/gentelella/vendors/bootstrap/dist/*js/bootstrap.min.js",
            "node_modules/ng-gentelella/*gentelella/gentelella.jquery.js"
        ],
        sass: [
            "node_modules/ng-gentelella/gentelella/*.s?ss"
        ],
        css: [
            "node_modules/gentelella/vendors/bootstrap/dist/css/bootstrap.css",
            "node_modules/font-awesome/css/font-awesome.css",
            "node_modules/gentelella/vendors/nprogress/nprogress.css",
            "node_modules/gentelella/vendors/iCheck/skins/flat/green.css",
            "node_modules/gentelella/vendors/bootstrap-progressbar/css/bootstrap-progressbar-3.3.4.min.css",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.css",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.buttons.css",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.nonblock.css",
            "node_modules/gentelella/vendors/select2/dist/css/select2.min.css",
        ],
        js_watch: [
            "node_modules/jquery/dist/jquery.js",
            "node_modules/angular/angular.js",
            "node_modules/angular-route/angular-route.js",
            "node_modules/angular-resource/angular-resource.js",
            "node_modules/angular-animate/angular-animate.js",
            "node_modules/angular-sanitize/angular-sanitize.js",
            "node_modules/ng-file-upload/dist/ng-file-upload.js",
            "node_modules/gentelella/vendors/fastclick/lib/fastclick.js",
            "node_modules/gentelella/vendors/nprogress/nprogress.js",
            "node_modules/gentelella/vendors/Chart.js/dist/Chart.min.js",
            "node_modules/gentelella/vendors/gauge.js/dist/gauge.min.js",
            "node_modules/gentelella/vendors/bootstrap-progressbar/bootstrap-progressbar.min.js",
            "node_modules/gentelella/vendors/iCheck/icheck.min.js",
            "node_modules/gentelella/vendors/skycons/skycons.js",
            "node_modules/gentelella/vendors/Flot/jquery.flot.js",
            "node_modules/gentelella/vendors/Flot/jquery.flot.pie.js",
            "node_modules/gentelella/vendors/Flot/jquery.flot.time.js",
            "node_modules/gentelella/vendors/Flot/jquery.flot.stack.js",
            "node_modules/gentelella/vendors/Flot/jquery.flot.resize.js",
            "node_modules/gentelella/production/js/flot/jquery.flot.orderBars.js",
            "node_modules/gentelella/production/js/flot/date.js",
            "node_modules/gentelella/production/js/flot/jquery.flot.spline.js",
            "node_modules/gentelella/production/js/flot/curvedLines.js",
            "node_modules/gentelella/production/js/moment/moment.min.js",
            "node_modules/gentelella/production/js/datepicker/daterangepicker.js",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.js",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.buttons.js",
            "node_modules/gentelella/vendors/pnotify/dist/pnotify.nonblock.js",
            "node_modules/gentelella/vendors/select2/dist/js/select2.full.js",

            "node_modules/ng-gentelella/gentelella/*.module.js",
            "node_modules/ng-gentelella/gentelella/*.config.js",
            "node_modules/ng-gentelella/gentelella/**/*.module.js",
            "node_modules/ng-gentelella/gentelella/**/*.component.js",
            "node_modules/ng-gentelella/gentelella/**/*.service.js"
        ],
        partials: [
            "node_modules/*ng-gentelella/gentelella/**/*.html"
        ],
        build: "target/js/admin"
    }
};
var config = {
    autoprefixer_versions: ["last 2 version", "safari 5", "ie 8", "ie 9"]
},
images = [];



"use strict";

var gulp = require( "gulp" ),
    gutil = require( "gulp-util" ),
    taskMethods = require( "gulpfile-ninecms"),
    argv = require( "yargs" ).argv;


var tasks = {
    clean: function () { return taskMethods.clean(paths); },
    assets: function () { return taskMethods.assets(paths); },
    css: function () { return taskMethods.css(paths); },
    less: function () { return taskMethods.less(paths); },
    sass: function () { return taskMethods.sass(paths); },
    browserify: function () { return taskMethods.browserify(paths); },
    lintJs: function () { return taskMethods.lintJs(paths); },
    concatJs: function () { return taskMethods.concatJs(paths); },
    preloadNgHtml: function () { return taskMethods.preloadNgHtml(paths); },
    images: function () { return taskMethods.images(paths, images); },
    clean_image_opts: function () { return taskMethods.clean_image_opts() },
    fonts: function () { return taskMethods.fonts(paths); },
    nsp: function () { return taskMethods.nsp(); },

    adminAssets: function () { return taskMethods.assets(paths.admin); },
    adminCss: function () { return taskMethods.css(paths.admin); },
    adminSass: function () { return taskMethods.sass(paths.admin); },
    adminConcatJs: function () { return taskMethods.concatJs(paths.admin); },
    adminPreloadNgHtml: function () { return taskMethods.preloadNgHtml(paths.admin); }
};

gulp.task("clean", tasks.clean);

// for production we require the clean method on every individual task
var build = argv._.length ? argv._[0] === "build" : false;
var req = build ? ["clean"] : [];

// individual tasks
gulp.task("assets", req, tasks.assets);
gulp.task("css", req.concat(["less", "sass"]), tasks.css);
gulp.task("less", req, tasks.less);
gulp.task("sass", req, tasks.sass);
gulp.task("browserify", req, tasks.browserify);
gulp.task("lintjs", tasks.lintjs);
gulp.task("concatJs", req, tasks.concatJs);
gulp.task("images", req, tasks.images);
gulp.task("clean_image_opts", req, tasks.clean_image_opts);
gulp.task("fonts", req, tasks.fonts);
gulp.task("nsp", tasks.nsp);
gulp.task("preTest", tasks.preTest);
gulp.task("mocha", tasks.mocha);
gulp.task("karma", tasks.karma);
gulp.task("istanbul", ["preTest"], tasks.mocha);
gulp.task("adminAssets", req, tasks.adminAssets);
gulp.task("adminSass", req, tasks.adminSass);
gulp.task("adminCss", req.concat(["adminSass"]), tasks.adminCss);
gulp.task("preloadNgHtml", req, tasks.preloadNgHtml);
gulp.task("adminConcatJs", req.concat(["preloadNgHtml"]), tasks.adminConcatJs);

// build task
gulp.task("build", [
    "assets",
    "less",
    "sass",
    "css",
    "browserify",
    "concatJs",
    "images",
    "fonts",
    "adminAssets",
    "adminSass",
    "adminCss",
    "adminConcatJs"
]);

// test task
gulp.task("test", [
    "lintjs",
    "nsp",
    "istanbul",
    "karma"
]);

// watch task
gulp.task("watch", ["build"], function () {
    gulp.watch(paths.css, ["css"]);
    gulp.watch(paths.less, ["less", "css"]);
    gulp.watch(paths.sass, ["sass", "css"]);
    gulp.watch(paths.js_watch, ["concatJs"]);
    gulp.watch(paths.admin.css, ["adminCss"]);
    gulp.watch(paths.admin.sass, ["adminSass", "adminCss"]);
    gulp.watch(paths.admin.js_watch, ["adminConcatJs"]);
    gulp.watch(paths.admin.partials, ["adminConcatJs"]);
    gulp.watch(["./fonts.list"], ["fonts"]);
    gutil.log(gutil.colors.bgGreen("Watching for changes..."));
});

gulp.task("default", ["watch"]);
