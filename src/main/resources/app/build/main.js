webpackJsonp([0],{

/***/ 102:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return StationPage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__app_backend_service__ = __webpack_require__(41);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var StationPage = (function () {
    function StationPage(navCtrl, backendService) {
        this.navCtrl = navCtrl;
        this.backendService = backendService;
        //this.backendService.getCompetitions();
    }
    Object.defineProperty(StationPage.prototype, "competition", {
        get: function () {
            return this.backendService.competition;
        },
        set: function (competitionId) {
            if (!this.stationFreezed) {
                this.backendService.getDurchgaenge(competitionId);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(StationPage.prototype, "durchgang", {
        get: function () {
            return this.backendService.durchgang;
        },
        set: function (d) {
            if (!this.stationFreezed) {
                this.backendService.getGeraete(this.competition, d);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(StationPage.prototype, "geraet", {
        get: function () {
            return this.backendService.geraet;
        },
        set: function (geraetId) {
            if (!this.stationFreezed) {
                this.backendService.getSteps(this.competition, this.durchgang, geraetId);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(StationPage.prototype, "step", {
        get: function () {
            return this.backendService.step;
        },
        set: function (s) {
            this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(StationPage.prototype, "stationFreezed", {
        get: function () {
            return this.backendService.stationFreezed;
        },
        enumerable: true,
        configurable: true
    });
    StationPage.prototype.getCompetitions = function () {
        return this.backendService.competitions;
    };
    StationPage.prototype.competitionName = function () {
        var _this = this;
        if (!this.backendService.competitions)
            return '';
        var candidate = this.backendService.competitions
            .filter(function (c) { return c.uuid === _this.backendService.competition; })
            .map(function (c) { return c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join("-"); });
        if (candidate.length === 1) {
            return candidate[0];
        }
        else {
            return '';
        }
    };
    StationPage.prototype.geraetName = function () {
        var _this = this;
        if (!this.backendService.geraete)
            return '';
        var candidate = this.backendService.geraete
            .filter(function (c) { return c.id === _this.backendService.geraet; })
            .map(function (c) { return c.name; });
        if (candidate.length === 1) {
            return candidate[0];
        }
        else {
            return '';
        }
    };
    StationPage.prototype.getDurchgaenge = function () {
        return this.backendService.durchgaenge;
    };
    StationPage.prototype.getGeraete = function () {
        return this.backendService.geraete;
    };
    StationPage.prototype.getSteps = function () {
        if (!this.backendService.steps && this.backendService.geraet) {
            this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
        }
        return this.backendService.steps;
    };
    StationPage.prototype.getWertungen = function () {
        return this.backendService.wertungen;
    };
    StationPage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'page-station',template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\station\station.html"*/'<ion-tabs>\n    <ion-tab tabIcon="settings" tabTitle="Station" [root]="tabStation"></ion-tab>\n    <ion-tab tabIcon="contact" tabTitle="Riege" [root]="tabRiege"></ion-tab>\n</ion-tabs>\n<ion-header>\n  <ion-navbar>\n    <button ion-button menuToggle>\n      <ion-icon name="menu"></ion-icon>\n    </button>\n    <ion-title>Resultaterfassung</ion-title>\n  </ion-navbar>\n</ion-header>\n\n<ion-content>\n    <riege-list *ngIf="geraet && durchgang && step && getWertungen()" [items] = \'getWertungen()\' [geraetId]=\'geraet\' [durchgang]=\'durchgang\' [step]=\'step\'></riege-list>\n</ion-content>\n\n<ion-footer >\n    <ion-toolbar>\n        <ion-list>\n          <ion-item >\n            <!-- <ion-thumbnail  item-start><img src="assets/imgs/wettkampf.png"></ion-thumbnail > -->\n            <ion-label>{{competitionName()}}</ion-label>\n          </ion-item>\n          <ion-item >\n            <ion-note *ngIf="durchgang && geraet">\n              {{durchgang}}, {{geraetName()}}, Riege {{step}}\n            </ion-note>\n          </ion-item>\n        </ion-list>      \n    </ion-toolbar>\n  \n</ion-footer>'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\station\station.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* NavController */], __WEBPACK_IMPORTED_MODULE_2__app_backend_service__["a" /* BackendService */]])
    ], StationPage);
    return StationPage;
}());

//# sourceMappingURL=station.js.map

/***/ }),

/***/ 113:
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncatched exception popping up in devtools
	return Promise.resolve().then(function() {
		throw new Error("Cannot find module '" + req + "'.");
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = 113;

/***/ }),

/***/ 156:
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncatched exception popping up in devtools
	return Promise.resolve().then(function() {
		throw new Error("Cannot find module '" + req + "'.");
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = 156;

/***/ }),

/***/ 200:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return HomePage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__app_backend_service__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__station_station__ = __webpack_require__(102);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var HomePage = (function () {
    function HomePage(navCtrl, backendService) {
        this.navCtrl = navCtrl;
        this.backendService = backendService;
        //this.backendService.getCompetitions();
    }
    Object.defineProperty(HomePage.prototype, "competition", {
        get: function () {
            return this.backendService.competition;
        },
        set: function (competitionId) {
            if (!this.stationFreezed) {
                this.backendService.getDurchgaenge(competitionId);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(HomePage.prototype, "durchgang", {
        get: function () {
            return this.backendService.durchgang;
        },
        set: function (d) {
            if (!this.stationFreezed) {
                this.backendService.getGeraete(this.competition, d);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(HomePage.prototype, "geraet", {
        get: function () {
            return this.backendService.geraet;
        },
        set: function (geraetId) {
            if (!this.stationFreezed) {
                this.backendService.getSteps(this.competition, this.durchgang, geraetId);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(HomePage.prototype, "step", {
        get: function () {
            return this.backendService.step;
        },
        set: function (s) {
            this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(HomePage.prototype, "stationFreezed", {
        get: function () {
            return this.backendService.stationFreezed;
        },
        enumerable: true,
        configurable: true
    });
    HomePage.prototype.getCompetitions = function () {
        return this.backendService.competitions;
    };
    HomePage.prototype.competitionName = function () {
        var _this = this;
        if (!this.backendService.competitions)
            return '';
        var candidate = this.backendService.competitions
            .filter(function (c) { return c.uuid === _this.backendService.competition; })
            .map(function (c) { return c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join("-"); });
        if (candidate.length === 1) {
            return candidate[0];
        }
        else {
            return '';
        }
    };
    HomePage.prototype.geraetName = function () {
        var _this = this;
        if (!this.backendService.geraete)
            return '';
        var candidate = this.backendService.geraete
            .filter(function (c) { return c.id === _this.backendService.geraet; })
            .map(function (c) { return c.name; });
        if (candidate.length === 1) {
            return candidate[0];
        }
        else {
            return '';
        }
    };
    HomePage.prototype.getDurchgaenge = function () {
        return this.backendService.durchgaenge;
    };
    HomePage.prototype.getGeraete = function () {
        return this.backendService.geraete;
    };
    HomePage.prototype.getSteps = function () {
        if (!this.backendService.steps && this.backendService.geraet) {
            this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
        }
        return this.backendService.steps;
    };
    HomePage.prototype.navToStation = function () {
        this.navCtrl.swipeBackEnabled = true;
        this.navCtrl.setRoot(__WEBPACK_IMPORTED_MODULE_3__station_station__["a" /* StationPage */]);
    };
    HomePage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'page-home',template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\home\home.html"*/'<!-- <ion-tabs>\n    <ion-tab tabIcon="settings" tabTitle="Station" [root]="tabStation"></ion-tab>\n    <ion-tab tabIcon="contact" tabTitle="Riege" [root]="tabRiege"></ion-tab>\n</ion-tabs> -->\n<ion-header>\n  <ion-navbar>\n    <button ion-button menuToggle>\n      <ion-icon name="menu"></ion-icon>\n    </button>\n    <ion-title>Wettkampf App</ion-title>\n  </ion-navbar>\n</ion-header>\n\n<ion-content>\n    <ion-list *ngIf="stationFreezed">\n        <ion-item >\n          <!-- <ion-thumbnail  item-start><img src="assets/imgs/wettkampf.png"></ion-thumbnail > -->\n          <ion-label>{{competitionName()}}</ion-label>\n        </ion-item>\n        <ion-item >\n          <ion-note *ngIf="durchgang && geraet">\n            {{durchgang}}, {{geraetName()}}\n          </ion-note>\n        </ion-item>\n        <ion-item>\n          <ion-range *ngIf="geraet && getSteps()" [(ngModel)]="step" pin="true" [min]="1" [max]="getSteps().length" [step]="1" [snaps]="true" color="secondary">\n              <ion-label range-left class="small-text">Riege 1</ion-label>\n              <ion-label range-right class="small-text">{{getSteps().length}}</ion-label>\n          </ion-range>\n          <ion-label item-start>Riege {{step}}</ion-label>\n        </ion-item>\n      </ion-list>      \n      <ion-list *ngIf="!stationFreezed">\n          <ion-item >\n            <!-- <ion-thumbnail  item-start><img src="assets/imgs/wettkampf.png"></ion-thumbnail > -->\n            <ion-label>Wettkampf</ion-label>\n            <ion-select *ngIf="getCompetitions()" [(ngModel)]=\'competition\' okText=\'Okay\' cancelText=\'Abbrechen\'>\n              <ion-option *ngFor=\'let comp of getCompetitions()\' [value]=\'comp.uuid\'>{{comp.titel}}</ion-option>\n            </ion-select>\n          </ion-item>\n          <ion-item >\n            <ion-label>Durchgang</ion-label>\n            <ion-select *ngIf="competition && getDurchgaenge()" [(ngModel)]=\'durchgang\' okText=\'Okay\' cancelText=\'Abbrechen\'>\n              <ion-option *ngFor=\'let d of getDurchgaenge()\' [value]=\'d\'>{{d}}</ion-option>\n            </ion-select>\n            <ion-note *ngIf="competition && !getDurchgaenge() && durchgang">\n              {{durchgang}}\n            </ion-note>\n          </ion-item>\n          <ion-item >\n            <ion-label>Gerät</ion-label>\n            <ion-select *ngIf="durchgang && getGeraete()" [(ngModel)]=\'geraet\' okText=\'Okay\' cancelText=\'Abbrechen\'>\n              <ion-option *ngFor=\'let g of getGeraete()\' [value]=\'g.id\'>{{g.name}}</ion-option>\n            </ion-select>\n            <ion-note *ngIf="competition && !getDurchgaenge() && durchgang">\n              {{geraet}}\n            </ion-note>\n          </ion-item>\n          <ion-item>\n            <ion-range *ngIf="geraet && getSteps()" [(ngModel)]="step" pin="true" [min]="1" [max]="getSteps().length" [step]="1" [snaps]="true" color="secondary">\n                <ion-label range-left class="small-text">Riege 1</ion-label>\n                <ion-label range-right class="small-text">{{getSteps().length}}</ion-label>\n            </ion-range>\n            <ion-label item-start>Riege {{step}}</ion-label>\n          </ion-item>\n        </ion-list>\n</ion-content>\n\n<ion-footer>\n    <ion-toolbar>\n        <ion-list>\n            <button ion-button large block icon-left color="secondary" (click)="navToStation()" [disabled]="!(geraet && getSteps())">\n                <ion-icon name="create"></ion-icon>Resultate</button>\n\n        </ion-list>\n    </ion-toolbar>\n  </ion-footer>'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\home\home.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* NavController */], __WEBPACK_IMPORTED_MODULE_2__app_backend_service__["a" /* BackendService */]])
    ], HomePage);
    return HomePage;
}());

//# sourceMappingURL=home.js.map

/***/ }),

/***/ 202:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return WertungEditorPage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__app_backend_service__ = __webpack_require__(41);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



/**
 * Generated class for the WertungEditorPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */
var WertungEditorPage = (function () {
    function WertungEditorPage(navCtrl, navParams, backendService) {
        this.navCtrl = navCtrl;
        this.navParams = navParams;
        this.backendService = backendService;
        // If we navigated to this page, we will have an item available as a nav param
        this.item = Object.assign({}, navParams.get('item'));
        this.itemOriginal = navParams.get('item');
        this.durchgang = navParams.get('durchgang');
        this.step = navParams.get('step');
        this.geraetId = navParams.get('geraetId');
    }
    WertungEditorPage.prototype.editable = function () {
        return this.backendService.loggedIn;
    };
    WertungEditorPage.prototype.save = function (wertung) {
        var _this = this;
        this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, wertung).subscribe(function (wc) {
            _this.item = Object.assign({}, wc);
            _this.navCtrl.pop();
        }, function (err) {
            _this.item = Object.assign({}, _this.itemOriginal);
            console.log(err);
        });
    };
    WertungEditorPage.prototype.geraetName = function () {
        var _this = this;
        return this.backendService.geraete ? this.backendService.geraete.find(function (g) { return g.id === _this.geraetId; }).name : '';
    };
    WertungEditorPage.prototype.ionViewDidLoad = function () {
        console.log('ionViewDidLoad WertungEditorPage');
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], WertungEditorPage.prototype, "item", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], WertungEditorPage.prototype, "geraetId", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], WertungEditorPage.prototype, "step", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], WertungEditorPage.prototype, "durchgang", void 0);
    WertungEditorPage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'page-wertung-editor',template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\wertung-editor\wertung-editor.html"*/'<!--\n  Generated template for the WertungEditorPage page.\n\n  See http://ionicframework.com/docs/components/#navigation for more info on\n  Ionic pages and navigation.\n-->\n<ion-header>\n  <ion-navbar>\n    <ion-title>\n      {{geraetName()}}\n    </ion-title>\n  </ion-navbar>\n</ion-header>\n\n<ion-content>\n  <ion-list>\n    <ion-item>\n        <ion-title>{{item.vorname + \' \' + item.name}}</ion-title>\n        <ion-note item-end>{{item.wertung.riege}}</ion-note>\n    </ion-item>\n    <ion-item>\n      <ion-label>D-Note</ion-label>\n      <ion-input [readonly]=\'!editable()\' [(ngModel)]=\'item.wertung.noteD\'></ion-input>\n    </ion-item>\n    <ion-item>\n      <ion-label>E-Note</ion-label>\n      <ion-input [readonly]=\'!editable()\' [(ngModel)]=\'item.wertung.noteE\'></ion-input>\n    </ion-item>\n    <ion-item>\n      <ion-label>Endnote</ion-label>\n      <ion-input [value]=\'item.wertung.endnote\' readonly></ion-input>\n    </ion-item>\n\n  </ion-list>\n</ion-content>\n\n<ion-footer>\n  <ion-toolbar>\n      <ion-list>\n          <button *ngIf=\'editable()\' ion-button large block icon-left color="secondary" (click)=\'save(item.wertung)\'>\n              <ion-icon name="checkmark-circle"></ion-icon>Save</button>\n      </ion-list>\n  </ion-toolbar>\n</ion-footer>\n'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\wertung-editor\wertung-editor.html"*/,
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* NavController */], __WEBPACK_IMPORTED_MODULE_1_ionic_angular__["f" /* NavParams */], __WEBPACK_IMPORTED_MODULE_2__app_backend_service__["a" /* BackendService */]])
    ], WertungEditorPage);
    return WertungEditorPage;
}());

//# sourceMappingURL=wertung-editor.js.map

/***/ }),

/***/ 203:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(204);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__app_module__ = __webpack_require__(225);


Object(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_1__app_module__["a" /* AppModule */]);
//# sourceMappingURL=main.js.map

/***/ }),

/***/ 225:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(27);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_component__ = __webpack_require__(266);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__pages_home_home__ = __webpack_require__(200);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__pages_list_riege_list__ = __webpack_require__(297);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ionic_native_status_bar__ = __webpack_require__(196);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ionic_native_splash_screen__ = __webpack_require__(199);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__angular_common_http__ = __webpack_require__(201);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__token_interceptor__ = __webpack_require__(298);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__backend_service__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__pages_wertung_editor_wertung_editor__ = __webpack_require__(202);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__pages_settings_settings__ = __webpack_require__(299);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__pages_station_station__ = __webpack_require__(102);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};














var AppModule = (function () {
    function AppModule() {
    }
    AppModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            declarations: [
                __WEBPACK_IMPORTED_MODULE_3__app_component__["a" /* MyApp */],
                __WEBPACK_IMPORTED_MODULE_4__pages_home_home__["a" /* HomePage */],
                __WEBPACK_IMPORTED_MODULE_13__pages_station_station__["a" /* StationPage */],
                __WEBPACK_IMPORTED_MODULE_12__pages_settings_settings__["a" /* SettingsPage */],
                __WEBPACK_IMPORTED_MODULE_5__pages_list_riege_list__["a" /* RiegeListPage */],
                __WEBPACK_IMPORTED_MODULE_11__pages_wertung_editor_wertung_editor__["a" /* WertungEditorPage */]
            ],
            imports: [
                __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
                __WEBPACK_IMPORTED_MODULE_8__angular_common_http__["c" /* HttpClientModule */],
                __WEBPACK_IMPORTED_MODULE_2_ionic_angular__["c" /* IonicModule */].forRoot(__WEBPACK_IMPORTED_MODULE_3__app_component__["a" /* MyApp */], {}, {
                    links: []
                }),
            ],
            bootstrap: [__WEBPACK_IMPORTED_MODULE_2_ionic_angular__["a" /* IonicApp */]],
            entryComponents: [
                __WEBPACK_IMPORTED_MODULE_3__app_component__["a" /* MyApp */],
                __WEBPACK_IMPORTED_MODULE_4__pages_home_home__["a" /* HomePage */],
                __WEBPACK_IMPORTED_MODULE_13__pages_station_station__["a" /* StationPage */],
                __WEBPACK_IMPORTED_MODULE_12__pages_settings_settings__["a" /* SettingsPage */],
                __WEBPACK_IMPORTED_MODULE_5__pages_list_riege_list__["a" /* RiegeListPage */],
                __WEBPACK_IMPORTED_MODULE_11__pages_wertung_editor_wertung_editor__["a" /* WertungEditorPage */]
            ],
            providers: [
                __WEBPACK_IMPORTED_MODULE_6__ionic_native_status_bar__["a" /* StatusBar */],
                __WEBPACK_IMPORTED_MODULE_7__ionic_native_splash_screen__["a" /* SplashScreen */],
                __WEBPACK_IMPORTED_MODULE_10__backend_service__["a" /* BackendService */],
                __WEBPACK_IMPORTED_MODULE_9__token_interceptor__["a" /* TokenInterceptor */],
                {
                    provide: __WEBPACK_IMPORTED_MODULE_8__angular_common_http__["a" /* HTTP_INTERCEPTORS */],
                    useClass: __WEBPACK_IMPORTED_MODULE_9__token_interceptor__["a" /* TokenInterceptor */],
                    multi: true
                },
                { provide: __WEBPACK_IMPORTED_MODULE_1__angular_core__["ErrorHandler"], useClass: __WEBPACK_IMPORTED_MODULE_2_ionic_angular__["b" /* IonicErrorHandler */] }
            ]
        })
    ], AppModule);
    return AppModule;
}());

//# sourceMappingURL=app.module.js.map

/***/ }),

/***/ 266:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MyApp; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ionic_native_status_bar__ = __webpack_require__(196);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__ = __webpack_require__(199);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__pages_home_home__ = __webpack_require__(200);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__pages_station_station__ = __webpack_require__(102);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






var MyApp = (function () {
    function MyApp(platform, statusBar, splashScreen) {
        this.platform = platform;
        this.statusBar = statusBar;
        this.splashScreen = splashScreen;
        this.rootPage = __WEBPACK_IMPORTED_MODULE_4__pages_home_home__["a" /* HomePage */];
        this.initializeApp();
        // used for an example of ngFor and navigation
        this.pages = [
            { title: 'Home', component: __WEBPACK_IMPORTED_MODULE_4__pages_home_home__["a" /* HomePage */] },
            { title: 'Resultate', component: __WEBPACK_IMPORTED_MODULE_5__pages_station_station__["a" /* StationPage */] } /*,
            { title: 'Settings', component: SettingsPage }*/
        ];
    }
    MyApp.prototype.initializeApp = function () {
        var _this = this;
        this.platform.ready().then(function () {
            // Okay, so the platform is ready and our plugins are available.
            // Here you can do any higher level native things you might need.
            _this.statusBar.styleDefault();
            if (window.location.href.indexOf('?') > 0) {
                try {
                    var initializeWith = atob(window.location.href.split('?')[1]);
                    localStorage.setItem("external_load", initializeWith);
                    window.history.replaceState({}, document.title, window.location.href.split('?')[0]);
                }
                catch (e) {
                    console.log(e);
                }
            }
            _this.splashScreen.hide();
        });
    };
    MyApp.prototype.openPage = function (page) {
        // Reset the content nav to have just this page
        // we wouldn't want the back button to show in this scenario
        this.nav.setRoot(page.component);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewChild"])(__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["d" /* Nav */]),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_1_ionic_angular__["d" /* Nav */])
    ], MyApp.prototype, "nav", void 0);
    MyApp = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\app\app.html"*/'<ion-menu [content]="content">\n  <ion-header>\n    <ion-toolbar>\n      <ion-title>Menu</ion-title>\n    </ion-toolbar>\n  </ion-header>\n\n  <ion-content>\n    <ion-list>\n      <button menuClose ion-item *ngFor="let p of pages" (click)="openPage(p)">\n        {{p.title}}\n      </button>\n    </ion-list>\n  </ion-content>\n\n</ion-menu>\n\n<!-- Disable swipe-to-go-back because it\'s poor UX to combine STGB with side menus -->\n<ion-nav [root]="rootPage" #content swipeBackEnabled="false"></ion-nav>'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\app\app.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["g" /* Platform */], __WEBPACK_IMPORTED_MODULE_2__ionic_native_status_bar__["a" /* StatusBar */], __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__["a" /* SplashScreen */]])
    ], MyApp);
    return MyApp;
}());

//# sourceMappingURL=app.component.js.map

/***/ }),

/***/ 280:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export availableLanguages */
/* unused harmony export defaultLanguage */
/* unused harmony export sysOptions */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return backendUrl; });
var availableLanguages = [{
        code: 'en',
        name: 'English'
        // }, {
        //   code: 'fr',
        //   name: 'French'
        // }, {
        //   code: 'it',
        //   name: 'Italien'
    }, {
        code: 'ge',
        name: 'German'
    }];
var defaultLanguage = 'de';
var sysOptions = {
    systemLanguage: defaultLanguage
};
var host = location.host;
var path = location.pathname;
var protocol = location.protocol;
var backendUrl = protocol + "//" + host + path;
//# sourceMappingURL=utils.js.map

/***/ }),

/***/ 297:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RiegeListPage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__wertung_editor_wertung_editor__ = __webpack_require__(202);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var RiegeListPage = (function () {
    function RiegeListPage(navCtrl) {
        this.navCtrl = navCtrl;
    }
    RiegeListPage.prototype.itemTapped = function (event, item) {
        // That's right, we're pushing to ourselves!
        this.navCtrl.push(__WEBPACK_IMPORTED_MODULE_2__wertung_editor_wertung_editor__["a" /* WertungEditorPage */], {
            item: item,
            durchgang: this.durchgang,
            step: this.step,
            geraetId: this.geraetId
        });
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], RiegeListPage.prototype, "items", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], RiegeListPage.prototype, "geraetId", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], RiegeListPage.prototype, "durchgang", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], RiegeListPage.prototype, "step", void 0);
    RiegeListPage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'riege-list',template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\list\riege-list.html"*/'<ion-list *ngIf="items && items.length > 0">\n  <ion-item *ngFor=\'let w of items\' (click)="itemTapped($event, w)">{{w.vorname + \' \' + w.name + \' (\' + w.wertung.riege + \')\'}}\n    <ion-avatar item-start>\n      <img src="assets/imgs/athlete.png">\n    </ion-avatar>\n    <ion-note item-end>{{w.wertung.endnote}}</ion-note>\n  </ion-item>\n</ion-list>\n'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\list\riege-list.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* NavController */]])
    ], RiegeListPage);
    return RiegeListPage;
}());

//# sourceMappingURL=riege-list.js.map

/***/ }),

/***/ 298:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TokenInterceptor; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var TokenInterceptor = (function () {
    function TokenInterceptor() {
    }
    TokenInterceptor.prototype.intercept = function (request, next) {
        request = request.clone({
            setHeaders: {
                'x-access-token': "" + localStorage.getItem('auth_token')
            }
        });
        return next.handle(request);
    };
    TokenInterceptor = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [])
    ], TokenInterceptor);
    return TokenInterceptor;
}());

//# sourceMappingURL=token-interceptor.js.map

/***/ }),

/***/ 299:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return SettingsPage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__app_backend_service__ = __webpack_require__(41);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


// import { TranslateService } from '@ngx-translate/core';

var SettingsPage = (function () {
    // get lastMessages():string[] {
    //   return this.ws.lastMessages;
    // };
    function SettingsPage(navCtrl, ws) {
        this.navCtrl = navCtrl;
        this.ws = ws;
        this.password = '';
        // ws.loginFailed.subscribe(lf => {
        //   this.loginFailed = lf.passwordRequired ? 'Password required' : 'User exists already';
        // });
    }
    Object.defineProperty(SettingsPage.prototype, "username", {
        get: function () {
            // return this.ws.getUsername();
            return "";
        },
        set: function (name) {
            // this.ws.setUsername(name);
        },
        enumerable: true,
        configurable: true
    });
    // connectedState(): Observable<boolean> {
    //   // return this.ws.connected;
    // }
    SettingsPage.prototype.loggedIn = function () {
        return this.ws.loggedIn;
    };
    // stopped() {
    //   this.loginFailed = '';
    //   return this.ws.stopped;
    // }
    // loggedInText(): Observable<any> {
    //   if (this.loginFailed) {
    //     return Observable.of(this.loginFailed);
    //   }
    //   return this.ws.identified.map(c => c ? this.translate.instant("messages.UserConnected", {"username": this.ws.getUsername()}) : this.translate.instant("messages.UserDisconnected", {"username": this.ws.getUsername()}));
    // }
    SettingsPage.prototype.logIn = function (name, password) {
        // this.subscr = this.ws.identified.subscribe(e => {
        //   if (e) {
        //     this.navCtrl.pop();
        //     this.subscr.unsubscribe();
        //   }
        // });
        this.ws.login(name, password);
    };
    SettingsPage.prototype.logOut = function () {
        this.loginFailed = '';
        this.ws.logout();
        // this.ws.disconnectWS();
    };
    SettingsPage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'page-settings',template:/*ion-inline-start:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\settings\settings.html"*/'<ion-header>\n  <ion-navbar color="primary">\n    <button ion-button menuToggle>\n      <ion-icon name="menu"></ion-icon>\n    </button>\n    <ion-buttons end>\n      <ion-note>Logged In: {{loggedIn()}}</ion-note>\n    </ion-buttons>\n  </ion-navbar>\n</ion-header>\n\n<ion-content>\n  <ion-item>\n    <ion-label floating>Name</ion-label>\n    <ion-input #issuerName [(ngModel)]="username" [disabled]="loggedIn()"></ion-input>\n  </ion-item>\n  <ion-item>\n    <ion-label floating>Passwort</ion-label>\n    <ion-input type="password" #issuerpassword [(ngModel)]="password" [disabled]="loggedIn()"></ion-input>\n  </ion-item>\n  <ion-item>\n    <button [disabled]="loggedIn()" ion-button large block icon-left color="secondary" (click)="logIn(issuerName.value, issuerpassword.value)">\n      <ion-icon name="log-in"></ion-icon>Login</button>\n    <button [disabled]="!loggedIn()" ion-button large block icon-left (click)="logOut()">\n      <ion-icon name="log-out"></ion-icon>Logout</button>\n  </ion-item>\n\n  <!-- <ion-card *ngIf="lastMessages">\n    <ion-item>\n      <h2>Last connection state-changes</h2>\n    </ion-item>\n    <hr>\n    <p *ngFor="let msg of lastMessages">{{msg}}</p>\n  </ion-card> -->\n</ion-content>'/*ion-inline-end:"c:\Users\Roland\git\KuTu\client\resultcatcher\src\pages\settings\settings.html"*/,
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* NavController */], __WEBPACK_IMPORTED_MODULE_2__app_backend_service__["a" /* BackendService */]])
    ], SettingsPage);
    return SettingsPage;
}());

//# sourceMappingURL=settings.js.map

/***/ }),

/***/ 41:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BackendService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common_http__ = __webpack_require__(201);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils__ = __webpack_require__(280);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Subject__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Subject___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_Subject__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_angular2_jwt__ = __webpack_require__(281);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_angular2_jwt___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_angular2_jwt__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_observable_interval__ = __webpack_require__(289);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_observable_interval___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_rxjs_observable_interval__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






function encodeURIComponent2(uri) {
    return encodeURIComponent(uri.replace(/[,&.*+?/^${}()|[\]\\]/g, "_"));
}
var BackendService = (function () {
    function BackendService(http) {
        var _this = this;
        this.http = http;
        this.loggedIn = false;
        this.stationFreezed = false;
        this._competition = undefined;
        this._durchgang = undefined;
        this._geraet = undefined;
        this._step = undefined;
        this.externalLoaderSubscription = Object(__WEBPACK_IMPORTED_MODULE_5_rxjs_observable_interval__["interval"])(1000).subscribe(function (latest) {
            _this.loggedIn = _this.checkJWT();
            var initWith = localStorage.getItem("external_load");
            if (initWith) {
                _this.initWithQuery(initWith);
            }
            else {
                _this.initWithQuery(localStorage.getItem('current_station'));
            }
        });
    }
    Object.defineProperty(BackendService.prototype, "competition", {
        get: function () {
            return this._competition;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BackendService.prototype, "durchgang", {
        get: function () {
            return this._durchgang;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BackendService.prototype, "geraet", {
        get: function () {
            return this._geraet;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BackendService.prototype, "step", {
        get: function () {
            return this._step;
        },
        enumerable: true,
        configurable: true
    });
    BackendService.prototype.initWithQuery = function (initWith) {
        var _this = this;
        if (initWith && initWith.startsWith('c=')) {
            initWith.split('&').forEach(function (param) {
                var _a = param.split('='), key = _a[0], value = _a[1];
                switch (key) {
                    case 's':
                        localStorage.setItem('auth_token', value);
                        _this.loggedIn = _this.checkJWT();
                        break;
                    case 'c':
                        _this._competition = value;
                        break;
                    case 'd':
                        _this._durchgang = value;
                        break;
                    case 'g':
                        _this._geraet = parseInt(value);
                        localStorage.setItem('current_station', initWith);
                        _this.stationFreezed = true;
                    default:
                        _this._step = 1;
                }
            });
            localStorage.removeItem("external_load");
            this.externalLoaderSubscription.unsubscribe();
            if (this._geraet) {
                this.getCompetitions();
                this.loadDurchgaenge();
                this.loadGeraete();
                this.loadSteps();
                this.loadWertungen();
            }
        }
    };
    BackendService.prototype.checkJWT = function () {
        return Object(__WEBPACK_IMPORTED_MODULE_4_angular2_jwt__["tokenNotExpired"])('auth_token');
    };
    BackendService.prototype.login = function (username, password) {
        var _this = this;
        var headers = new __WEBPACK_IMPORTED_MODULE_1__angular_common_http__["d" /* HttpHeaders */]();
        this.http.options(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/login', {
            observe: 'response',
            headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password)),
            withCredentials: true,
            responseType: 'text'
        }).subscribe(function (data) {
            console.log(data);
            localStorage.setItem('auth_token', data.headers.get('x-access-token'));
            _this.loggedIn = _this.checkJWT();
        }, function (err) {
            console.log(err);
            localStorage.removeItem('auth_token');
            _this.loggedIn = _this.checkJWT();
        });
    };
    BackendService.prototype.logout = function () {
        localStorage.removeItem('auth_token');
        this.loggedIn = this.checkJWT();
    };
    BackendService.prototype.getCompetitions = function () {
        var _this = this;
        this.http.get(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/competition').subscribe(function (data) {
            _this.competitions = data;
        });
    };
    BackendService.prototype.getDurchgaenge = function (competitionId) {
        if (this.durchgaenge != undefined && this._competition === competitionId)
            return;
        this.durchgaenge = undefined;
        this.geraete = undefined;
        this.steps = undefined;
        this.wertungen = undefined;
        this._competition = competitionId;
        this._durchgang = undefined;
        this._geraet = undefined;
        this._step = undefined;
        this.loadDurchgaenge();
    };
    BackendService.prototype.loadDurchgaenge = function () {
        var _this = this;
        this.http.get(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/durchgang/' + this._competition).subscribe(function (data) {
            _this.durchgaenge = data;
        });
    };
    BackendService.prototype.getGeraete = function (competitionId, durchgang) {
        if (this.geraete != undefined && this._competition === competitionId && this._durchgang === durchgang)
            return;
        this.geraete = undefined;
        this.steps = undefined;
        this.wertungen = undefined;
        this._competition = competitionId;
        this._durchgang = durchgang;
        this._geraet = undefined;
        this._step = undefined;
        this.loadGeraete();
    };
    BackendService.prototype.loadGeraete = function () {
        var _this = this;
        this.http.get(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang)).subscribe(function (data) {
            _this.geraete = data;
        });
    };
    BackendService.prototype.getSteps = function (competitionId, durchgang, geraetId) {
        if (this.steps != undefined && this._competition === competitionId && this._durchgang === durchgang && this._geraet === geraetId)
            return;
        this.steps = undefined;
        this.wertungen = undefined;
        this._competition = competitionId;
        this._durchgang = durchgang;
        this._geraet = geraetId;
        this._step = undefined;
        this.loadSteps();
    };
    BackendService.prototype.loadSteps = function () {
        var _this = this;
        this.http.get(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet).subscribe(function (data) {
            _this.steps = data.map(function (step) { return parseInt(step); });
            _this._step = 1;
        });
    };
    BackendService.prototype.getWertungen = function (competitionId, durchgang, geraetId, step) {
        if (this.wertungen != undefined && this._competition === competitionId && this._durchgang === durchgang && this._geraet === geraetId && this._step === step)
            return;
        this.wertungen = undefined;
        this._competition = competitionId;
        this._durchgang = durchgang;
        this._geraet = geraetId;
        this._step = step;
        this.loadWertungen();
    };
    BackendService.prototype.loadWertungen = function () {
        var _this = this;
        this.http.get(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet + '/' + this._step).subscribe(function (data) {
            _this.wertungen = data;
        });
    };
    BackendService.prototype.updateWertung = function (durchgang, step, geraetId, wertung) {
        var _this = this;
        var competitionId = wertung.wettkampfUUID;
        var result = new __WEBPACK_IMPORTED_MODULE_3_rxjs_Subject__["Subject"]();
        this.http.put(__WEBPACK_IMPORTED_MODULE_2__utils__["a" /* backendUrl */] + 'api/durchgang/' + competitionId + '/' + encodeURIComponent2(durchgang) + '/' + geraetId + '/' + step, wertung)
            .subscribe(function (data) {
            _this.wertungen = data;
            result.next(data.find(function (wc) { return wc.wertung.id === wertung.id; }));
            result.complete();
        }, function (err) {
            result.next(_this.wertungen.find(function (wc) { return wc.wertung.id === wertung.id; }));
            result.error(err);
        });
        return result;
    };
    BackendService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_common_http__["b" /* HttpClient */]])
    ], BackendService);
    return BackendService;
}());

//# sourceMappingURL=backend.service.js.map

/***/ })

},[203]);
//# sourceMappingURL=main.js.map