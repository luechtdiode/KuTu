"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[1053],{1053:(v,u,s)=>{s.r(u),s.d(u,{RegJudgeEditorPageModule:()=>Z});var m=s(9808),d=s(2382),i=s(3349),l=s(1631),e=s(7587),h=s(600);function p(o,g){if(1&o){const t=e.EpF();e.TgZ(0,"ion-content")(1,"form",6,7),e.NdJ("ngSubmit",function(){e.CHM(t);const r=e.MAs(2);return e.oxw().save(r.value)})("keyup.enter",function(){e.CHM(t);const r=e.MAs(2);return e.oxw().save(r.value)}),e.TgZ(3,"ion-list")(4,"ion-item-divider",8),e._uU(5,"Wertungsrichter/-in"),e.qZA(),e.TgZ(6,"ion-item")(7,"ion-avatar",0),e._UZ(8,"img",9),e.qZA(),e.TgZ(9,"ion-input",10),e.NdJ("ngModelChange",function(r){return e.CHM(t),e.oxw().registration.name=r}),e.qZA(),e.TgZ(10,"ion-input",11),e.NdJ("ngModelChange",function(r){return e.CHM(t),e.oxw().registration.vorname=r}),e.qZA()(),e.TgZ(11,"ion-item")(12,"ion-avatar",0),e._uU(13," \xa0 "),e.qZA(),e.TgZ(14,"ion-label",8),e._uU(15,"Geschlecht"),e.qZA(),e.TgZ(16,"ion-select",12),e.NdJ("ngModelChange",function(r){return e.CHM(t),e.oxw().registration.geschlecht=r}),e.TgZ(17,"ion-select-option",13),e._uU(18,"weiblich"),e.qZA(),e.TgZ(19,"ion-select-option",13),e._uU(20,"m\xe4nnlich"),e.qZA()()(),e.TgZ(21,"ion-item-divider",8),e._uU(22,"Kontakt"),e.qZA(),e.TgZ(23,"ion-item")(24,"ion-avatar",0),e._UZ(25,"img",14),e.qZA(),e._UZ(26,"ion-input",15)(27,"ion-input",16),e.qZA(),e.TgZ(28,"ion-item-divider")(29,"ion-avatar",0),e._UZ(30,"img",17),e.qZA(),e.TgZ(31,"ion-label"),e._uU(32,"Einteilung"),e.qZA()(),e.TgZ(33,"ion-item")(34,"ion-avatar",0),e._uU(35," \xa0 "),e.qZA(),e.TgZ(36,"ion-label",8),e._uU(37,"Kommentar"),e.qZA(),e.TgZ(38,"ion-textarea",18),e.NdJ("ngModelChange",function(r){return e.CHM(t),e.oxw().registration.comment=r}),e.qZA()()(),e.TgZ(39,"ion-list")(40,"ion-button",19,20),e._UZ(42,"ion-icon",21),e._uU(43,"Speichern"),e.qZA()()()()}if(2&o){const t=e.MAs(2),n=e.oxw();e.xp6(9),e.Q6J("disabled",!1)("ngModel",n.registration.name),e.xp6(1),e.Q6J("disabled",!1)("ngModel",n.registration.vorname),e.xp6(6),e.Q6J("ngModel",n.registration.geschlecht),e.xp6(1),e.Q6J("value","W"),e.xp6(2),e.Q6J("value","M"),e.xp6(7),e.Q6J("readonly",!1)("disabled",!1)("ngModel",n.registration.mail),e.xp6(1),e.Q6J("readonly",!1)("disabled",!1)("ngModel",n.registration.mobilephone),e.xp6(11),e.Q6J("disabled",!1)("ngModel",n.registration.comment),e.xp6(2),e.Q6J("disabled",n.waiting||!t.valid||t.untouched)}}function _(o,g){if(1&o){const t=e.EpF();e.TgZ(0,"ion-button",22,23),e.NdJ("click",function(){return e.CHM(t),e.oxw().delete()}),e._UZ(2,"ion-icon",24),e._uU(3,"Anmeldung l\xf6schen"),e.qZA()}if(2&o){const t=e.oxw();e.Q6J("disabled",t.waiting)}}const b=[{path:"",component:(()=>{class o{constructor(t,n,r,a,c){this.navCtrl=t,this.route=n,this.backendService=r,this.alertCtrl=a,this.zone=c,this.waiting=!1}ngOnInit(){this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.judgeId=parseInt(this.route.snapshot.paramMap.get("judgeId")),this.backendService.getCompetitions().subscribe(t=>{const n=t.find(r=>r.uuid===this.wkId);this.wettkampfId=parseInt(n.id),this.backendService.loadProgramsForCompetition(n.uuid).subscribe(r=>{this.wkPgms=r,this.judgeId?this.backendService.loadJudgeRegistrations(this.wkId,this.regId).subscribe(a=>{this.updateUI(a.find(c=>c.id===this.judgeId))}):this.updateUI({id:0,vereinregistrationId:this.regId,name:"",vorname:"",mobilephone:"+417",mail:"",comment:"",registrationTime:0})})})}editable(){return this.backendService.loggedIn}updateUI(t){this.zone.run(()=>{this.waiting=!1,this.wettkampf=this.backendService.competitionName,this.registration=t})}save(t){const n=Object.assign({},this.registration,t);console.log(n),0===this.judgeId||0===n.id?this.backendService.createJudgeRegistration(this.wkId,this.regId,n).subscribe(()=>{this.navCtrl.pop()}):this.backendService.saveJudgeRegistration(this.wkId,this.regId,n).subscribe(()=>{this.navCtrl.pop()})}delete(){this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Judge-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteJudgeRegistration(this.wkId,this.regId,this.registration).subscribe(()=>{this.navCtrl.pop()})}}]}).then(n=>n.present())}}return o.\u0275fac=function(t){return new(t||o)(e.Y36(i.SH),e.Y36(l.gz),e.Y36(h.v),e.Y36(i.Br),e.Y36(e.R0b))},o.\u0275cmp=e.Xpm({type:o,selectors:[["app-reg-judge-editor"]],decls:14,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"judge"],[4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["judgeRegistrationForm","ngForm"],["color","primary"],["src","assets/imgs/chief.png"],["required","","placeholder","Name","type","text","name","name","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Vorname","type","text","name","vorname","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Geschlecht","name","geschlecht","okText","Okay","cancelText","Abbrechen","required","",3,"ngModel","ngModelChange"],[3,"value"],["src","assets/imgs/atmail.png"],["placeholder","Mail","type","email","pattern","^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,5})$","name","mail","required","",3,"readonly","disabled","ngModel"],["placeholder","Mobile/SMS (+4179...)","type","tel","pattern","[+]{1}[0-9]{11,14}","name","mobilephone","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/wettkampf.png"],["placeholder","Kommentar zur Einteilung als Wertungsrichter","name","comment",3,"disabled","ngModel","ngModelChange"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name",""],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","trash"]],template:function(t,n){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title"),e._uU(5,"Wertungsrichter-Anmeldung"),e.qZA(),e.TgZ(6,"ion-note",2)(7,"div",3),e._uU(8),e.qZA()()()(),e.YNc(9,p,44,16,"ion-content",4),e.TgZ(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list"),e.YNc(13,_,4,1,"ion-button",5),e.qZA()()()),2&t&&(e.xp6(8),e.hij("f\xfcr ",n.wettkampf,""),e.xp6(1),e.Q6J("ngIf",n.registration),e.xp6(4),e.Q6J("ngIf",n.judgeId>0))},directives:[i.Gu,i.sr,i.Sm,i.oU,i.cs,i.wd,i.uN,m.O5,i.W2,d._Y,d.JL,d.F,i.q_,i.rH,i.Ie,i.BJ,i.pK,i.j9,d.Q7,d.JJ,d.On,i.Q$,i.t9,i.QI,i.n0,d.c5,i.g2,i.YG,i.gu,i.fr],styles:[""]}),o})()}];let f=(()=>{class o{}return o.\u0275fac=function(t){return new(t||o)},o.\u0275mod=e.oAB({type:o}),o.\u0275inj=e.cJS({imports:[[l.Bz.forChild(b)],l.Bz]}),o})(),Z=(()=>{class o{}return o.\u0275fac=function(t){return new(t||o)},o.\u0275mod=e.oAB({type:o}),o.\u0275inj=e.cJS({imports:[[m.ez,d.u5,i.Pc,f]]}),o})()}}]);