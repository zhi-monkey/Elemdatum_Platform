<template>
  <div class="constain" id="pageBorder">
    <border-box11 title="透明化煤流">
      <div style="height: 100vh; width: 100%; display: flex; flex-direction: row">
        <div style="height: 100vh; width: 72%; position: relative">
          <!--          上仓胶带-->
          <div style="position: relative; height: 25vh; width: 15vw; top: 20%; left: 27%">
            <el-tooltip class="box-item" placement="right" popper-class="share_atooltip">
              <template #content>
                <border-box1>
                  <el-row style="width: 400px; height: 180px">
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ kamikura[0].scene }}{{ kamikura[0].modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ kamikura[0].data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ kamikura[0].scene }}皮带速度 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ kamikura[0].data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ kamikura[0].scene }}煤流量 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ kamikura[0].data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ kamikura[0].scene }}{{ kamikura[0].modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10">
                      <water-level-pond
                        :config="config1"
                        style="width: 100px; height: 50px; top: -10%; left: 20%"
                      />
                    </el-col>
                  </el-row>
                </border-box1>
              </template>
              <el-image
                :src="url1"
                style="height: 7.5vh; width: 15vw; left: 10%; top: 30%; transform: rotate(30deg)"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">上仓一号摄像头 </el-col>
                </el-row>
              </template>
              <el-image
                :src="shexiangUrl1"
                style="height: 6vh; width: 4vh; position: absolute; left: 50%; top: -10%"
                class="test"
                @click="kamikuraChange()"
                v-show="isBe"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle"> 上仓二号摄像头 </el-col>
                </el-row>
              </template>
              <el-image
                :src="shexiangUrl2"
                style="height: 6; width: 4vh; position: absolute; left: 80%; top: 10%"
                @click="kamikuraChange()"
                v-show="isBe"
              />
            </el-tooltip>

            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">上仓主机 </el-col>
                </el-row>
              </template>
              <el-image
                :src="DiannaoUrl1"
                style="height: 6vh; width: 4vh; position: absolute; left: 65%; top: 0%"
                @click="getComputerInfo1()"
                v-show="isBe1"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">上仓纵撕 </el-col>
                </el-row>
              </template>
              <el-image
                :src="warnUrl1"
                style="height: 8vh; width: 5vh; position: absolute; left: 40%; top: 70%"
                @click="WarnningChange1()"
              />
            </el-tooltip>
          </div>
          <!--主井胶带-->
          <div style="position: relative; height: 20vh; width: 15vw; top: 2%; left: 46%">
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <border-box1>
                  <el-row style="width: 400px; height: 180px">
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ mainWell[0].scene }}{{ mainWell[0].modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainWell[0].data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ mainWell[0].scene }}皮带速度 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainWell[0].data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ mainWell[0].scene }}煤流量 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainWell[0].data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ mainWell[0].scene }}{{ mainWell[0].modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10">
                      <water-level-pond
                        :config="config2"
                        style="width: 100px; height: 50px; top: -10%; left: 20%"
                      />
                    </el-col>
                  </el-row>
                </border-box1>
              </template>
              <el-image
                :src="url2"
                style="height: 7.5vh; width: 15vw; left: 0%; top: 25%; transform: rotate(30deg)"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">主井一号摄像头 </el-col>
                </el-row>
              </template>
              <el-image
                :src="shexiangUrl3"
                style="height: 6vh; width: 4vh; position: absolute; left: 50%; top: -10%"
                @click="mainWellChange()"
                v-show="isBe"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">主井二号摄像头 </el-col>
                </el-row>
              </template>
              <el-image
                :src="shexiangUrl4"
                style="height: 6vh; width: 4vh; position: absolute; left: 80%; top: 10%"
                @click="mainWellChange()"
                v-show="isBe"
              />
            </el-tooltip>
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" class="fontStyle">主井主机 </el-col>
                </el-row>
              </template>
              <el-image
                :src="DiannaoUrl2"
                style="height: 6vh; width: 4vh; position: absolute; left: 65%; top: 0%"
                @click="getComputerInfo2()"
                v-show="isBe1"
              />
            </el-tooltip>

            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <el-row>
                  <el-col :span="24" style="color: rgb(12, 113, 228); font-size: 16px"
                    >主井纵撕
                  </el-col>
                </el-row>
              </template>
              <el-image
                :src="warnUrl2"
                style="height: 8vh; width: 5vh; position: absolute; left: 30%; top: 70%"
                @click="WarnningChange2()"
              />
            </el-tooltip>
          </div>
          <!--主运胶带-->
          <div style="position: relative; height: 40vh; width: 10vw; left: 60%; top: -2%">
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <border-box1>
                  <el-row style="width: 400px; height: 180px">
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ mainTrans[0].scene }}{{ mainTrans[0].modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainTrans[0].data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ mainTrans[0].scene }}皮带速度 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainTrans[0].data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ mainTrans[0].scene }}煤流量 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ mainTrans[0].data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ mainTrans[0].scene }}{{ mainTrans[0].modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10">
                      <water-level-pond
                        :config="config2"
                        style="width: 100px; height: 50px; top: -10%; left: 20%"
                      />
                    </el-col>
                  </el-row>
                </border-box1>
              </template>
              <el-image
                :src="url3"
                style="height: 9vh; width: 25vw; left: -65%; top: 30%; transform: rotate(90deg)"
              />
            </el-tooltip>
            <el-image
              :src="shexiangUrl5"
              style="height: 6vh; width: 4vh; position: absolute; left: 85%; top: 0%"
              @click="mainTransChange()"
              v-show="isBe"
            />
            <el-image
              :src="shexiangUrl6"
              style="height: 6vh; width: 4vh; position: absolute; left: 85%; top: 16%"
              @click="mainTransChange()"
              v-show="isBe"
            />
            <el-image
              :src="DiannaoUrl3"
              style="height: 6vh; width: 4vh; position: absolute; left: 85%; top: 32%"
              @click="getComputerInfo3()"
              v-show="isBe1"
            />
            <el-image
              :src="shexiangUrl7"
              style="height: 6vh; width: 4vh; position: absolute; left: 85%; top: 50%"
              @click="mainTransChange()"
              v-show="isBe"
            />
            <el-image
              :src="shexiangUrl8"
              style="height: 6vh; width: 4vh; position: absolute; left: 85%; top: 66%"
              @click="mainTransChange()"
              v-show="isBe"
            />
            <el-image
              :src="warnUrl3"
              style="height: 8vh; width: 5vh; position: absolute; left: -15%; top: 30%"
              @click="WarnningChange3()"
            />
            <el-image
              :src="warnUrl3"
              style="height: 8vh; width: 5vh; position: absolute; left: -15%; top: 60%"
              @click="WarnningChange3()"
            />
            <el-image
              :src="zhujiUrl"
              style="height: 8vh; width: 5vh; position: absolute; left: 70%; top: -40%"
            />
          </div>
          <!--综采-->
          <div style="position: relative; height: 10vh; width: 20vw; left: 82%; top: -39%">
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <border-box1>
                  <el-row style="width: 400px; height: 180px">
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ generalMining[0].scene }}{{ generalMining[0].modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ generalMining[0].data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle">
                      {{ generalMining[0].scene }}皮带速度
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ generalMining[0].data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle">
                      {{ generalMining[0].scene }}煤流量
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ generalMining[0].data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ generalMining[0].scene }}{{ generalMining[0].modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10">
                      <water-level-pond
                        :config="config1"
                        style="width: 100px; height: 50px; top: -10%; left: 20%"
                      />
                    </el-col>
                  </el-row>
                </border-box1>
              </template>
              <el-image :src="url4" style="height: 9vh; width: 20vw; left: -15%; top: 10%" />
            </el-tooltip>
            <el-image
              :src="shexiangUrl9"
              style="height: 6vh; width: 4vh; position: absolute; left: 0%; top: -60%"
              @click="generalMiningchange()"
              v-show="isBe"
            />
            <el-image
              :src="shexiangUrl10"
              style="height: 6vh; width: 4vh; position: absolute; left: 15%; top: -60%"
              @click="generalMiningchange()"
              v-show="isBe"
            />
            <el-image
              :src="warnUrl4"
              style="height: 6vh; width: 4vh; position: absolute; left: 30%; top: -60%"
              @click="WarnningChange4()"
              v-show="isBe"
            />
            <el-image
              :src="DiannaoUrl4"
              style="height: 6vh; width: 4vh; position: absolute; left: 45%; top: -60%"
              @click="getComputerInfo4()"
              v-show="isBe1"
            />
          </div>
          <!--掘进-->
          <div
            style="
              position: relative;
              height: 12vh;
              width: 20vw;

              left: 82%;
              top: -34%;
            "
          >
            <el-tooltip
              class="box-item"
              effect="dark"
              placement="right"
              popper-class="share_atooltip"
            >
              <template #content>
                <border-box1>
                  <el-row style="width: 400px; height: 180px">
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ digging[0].scene }}{{ digging[0].modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ digging[0].data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ digging[0].scene }}皮带速度 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ digging[0].data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ digging[0].scene }}煤流量 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ digging[0].data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ digging[0].scene }}{{ digging[0].modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10">
                      <water-level-pond
                        :config="config2"
                        style="width: 100px; height: 50px; top: -10%; left: 20%"
                      />
                    </el-col>
                  </el-row>
                </border-box1>
              </template>
              <el-image :src="url5" style="height: 9vh; width: 16vw; left: -15%; top: 25%" />
            </el-tooltip>
            <el-image
              :src="shexiangUrl12"
              style="height: 6vh; width: 4vh; position: absolute; left: 0%; top: -40%"
              @click="diggingchange()"
              v-show="isBe"
            />
            <el-image
              :src="shexiangUrl13"
              style="height: 6vh; width: 4vh; position: absolute; left: 20%; top: -40%"
              @click="getComputerInfo5()"
              v-show="isBe"
            />
            <el-image
              :src="warnUrl5"
              style="height: 6vh; width: 4vh; position: absolute; left: 40%; top: -40%"
              @click="WarnningChange5()"
              v-show="isBe"
            />
          </div>
          <!--          <div style="position: relative; top: -40%; left: 60%">-->
          <!--            <el-button type="primary" @click="show1()">只看摄像</el-button>-->
          <!--            <el-button type="primary" @click="show2()">只看主机</el-button>-->
          <!--          </div>-->

          <div style="width: 22vw; height: 32vh; left: 0%; position: relative; top: -48%">
            <border-box10>
              <el-row>
                <el-col :span="12" :offset="6" style="font-size: 30px; color: rgb(12, 113, 228)"
                  >{{ SelectName }}
                </el-col>
                <el-col
                  :span="8"
                  :offset="3"
                  v-for="item in selectInfo"
                  style="font-size: 40px; color: rgb(127, 187, 255)"
                  @click="changeInfo(item)"
                >
                  <el-button type="primary" style="width: 148px; height: 40px"
                    ><el-icon><View /></el-icon> {{ item.scene }}{{ item.monitorName }}</el-button
                  ></el-col
                >
              </el-row>
            </border-box10>
          </div>
          <div style="width: 22vw; height: 30vh; position: relative; top: -133%; left: 0%">
            <el-carousel height="260px">
              <el-carousel-item v-for="item in showInfo" :key="item">
                <border-box1>
                  <el-row style="width: 22vw; height: 260px">
                    <el-col :span="24" class="fontStyle">皮带档位信息</el-col>
                    <el-col :span="24" />
                    <el-col :span="13" class="fontStyle">
                      {{ item.scene }}{{ item.modelName }}
                    </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ item.data.gear }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ item.scene }}皮带速度 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ item.data.speed }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"> {{ item.scene }}煤流量 </el-col>
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ item.data.coalFlow }}
                    </el-col>

                    <el-col :span="13" class="fontStyle"
                      >{{ item.scene }}{{ item.modelName }}满载比例</el-col
                    >
                    <el-col :span="1" class="fontStyle">:</el-col>
                    <el-col :span="10" class="fontStyle">
                      {{ item.data.fullLoadRatio }}
                      <!--                      <water-level-pond-->
                      <!--                        :config="config1"-->
                      <!--                        style="width: 100px; height: 50px; top: -10%; left: 20%"-->
                      <!--                      />-->
                    </el-col>
                  </el-row>
                </border-box1>
              </el-carousel-item>
            </el-carousel></div
          >
          <div style="height: 30%; width: 22vw; position: relative; top: -138%">
            <scroll-board :config="config" style="width: 22vw; height: 28vh" />
          </div>
        </div>
        <div style="height: 100%; width: 22%; position: relative; left: 6%">
          <div style="height: 50%; width: 100%; top: 5%; position: relative">
            <border-box1>
              <div style="text-align: center; font-size: 24px; color: rgb(127, 255, 249)"
                >{{ camera.scene }}{{ camera.monitorName }}
              </div>
              <div style="height: 60%; width: 100%">
                <video
                  :src="videoUrl"
                  autoplay
                  muted
                  playsinline
                  loop
                  style="height: 90%; width: 95%; left: 2.5%; position: relative"
                >
                </video>
              </div>
              <div
                style="
                  height: 20%;
                  width: 100%;
                  font-size: 22px;
                  top: 0%;
                  position: relative;
                  font-style: oblique;
                "
              >
                <el-row style="width: 400px; height: 180px">
                  <el-col :span="12" class="fontStyle">
                    {{ camera.algName }}
                  </el-col>
                  <el-col :span="1" class="fontStyle">:</el-col>
                  <el-col :span="10" class="fontStyle">
                    {{ camera.customInfo.momentVolume }}立方分米
                  </el-col>
                  <el-col :span="12" class="fontStyle">{{ camera.algName }}满载比例</el-col>
                  <el-col :span="1" class="fontStyle">:</el-col>
                  <el-col :span="10">
                    <water-level-pond
                      :config="config1"
                      style="width: 100px; height: 50px; top: -10%; left: 20%"
                    />
                  </el-col>
                </el-row>
              </div>
            </border-box1>
          </div>
          <div style="height: 50%; width: 100%">
            <div class="echarts1" style="background-color: #181d31d0">
              <div ref="myChart" id="myChart" :style="{ width: '100%', height: '38vh' }"></div>
            </div>
          </div>
        </div>
      </div>
    </border-box11>

    <el-dialog
      v-model="visible"
      :show-close="false"
      width="40%"
      height="600px"
      style="background-color: #272f47; border-radius: 2px; padding: 0"
    >
      <template #header>
        <span style="font-size: 30px; position: relative; left: 40%; color: rgb(166, 169, 178)">
          {{ zongsiInfo }}
        </span>
      </template>
      <div>
        <el-table
          :data="warningInfo"
          height="300px"
          style="width: 100%"
          :cell-style="cellStyle"
          :default-sort="{ prop: 'address', order: 'descending' }"
          :header-cell-style="{
            background: '#31384c !important',
            color: '#ffffff',
            border: 'none !important',
            'text-align': 'center',
          }"
        >
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column prop="description" label="报警位置" width="180">
            <template #default="scope">
              <div :style="{ color: 'rgb(255, 233, 156)' }">
                {{ scope.row.description }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="操作" width="180">
            <template #default="scope">
              <el-button :type="getcolor(scope)" size="small">
                {{ scope.row.status }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="查看">
            <template #default="scope">
              <el-button type="primary" size="small" @click.prevent="search(scope)">
                查看
              </el-button>
              <!--              <el-button type="warning" size="small" @click.prevent="hulve(scope)">-->
              <!--                忽略-->
              <!--              </el-button>-->
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button
          type="danger"
          @click="close()"
          style="padding: 0px 20px 0px 10px; position: relative; left: -4%"
        >
          <el-icon class="el-icon--bottom">
            <CircleCloseFilled />
          </el-icon>
          Close
        </el-button>
      </template>
    </el-dialog>

    <!--    <BasicModal title="Modal Title" ><BasicModal/>-->
    <el-dialog v-model="visible1" width="60%" height="600px">
      <div style="flex-direction: row; display: flex">
        <div style="position: relative; width: 80%">
          <el-image :src="imageurl" style="width: 100%; height: 500px; position: relative" />
        </div>
        <div style="position: relative; width: 20%" class="fontStyle">
          <el-row style="text-align: center">
            <el-col style="font-size: 30px; margin-bottom: 20px">报警信息</el-col>
            <el-col style="margin-bottom: 10px">裂纹长度:{{ warnInfoData.tearLength }}</el-col>
            <el-col style="margin-bottom: 10px">裂纹宽度:{{ warnInfoData.tearWidth }}</el-col>
            <el-col style="margin-bottom: 10px">裂纹深度:{{ warnInfoData.tearDeep }}</el-col>
            <el-col style="margin-bottom: 10px">裂纹堆叠高度：{{ warnInfoData.tearHight }}</el-col>
          </el-row>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
  import { ref, onMounted, getCurrentInstance, reactive } from 'vue';
  import {
    BorderBox11,
    BorderBox10,
    BorderBox1,
    WaterLevelPond,
    ScrollBoard,
  } from '@kjgl77/datav-vue3';
  import { maHttp } from '../../../../utils/http/axios';
  import { MaBackendUrlEnum } from '../../../../enums/mineaiEnum';
  import { View } from '@element-plus/icons-vue';

  // import elementResizeDetectorMaker from 'element-resize-detector';
  // import debounce from 'lodash/debounce';

  // const index = ref(0);
  // let erd: elementResizeDetectorMaker.Erd = elementResizeDetectorMaker();
  const config = reactive({
    header: ['时间', '报警位置', '处理'],
    data: [
      ['行1列1', '行1列2', '行1列3'],
      ['行2列1', '行2列2', '行2列3'],
      ['行3列1', '行3列2', '行3列3'],
      ['行4列1', '行4列2', '行4列3'],
      ['行5列1', '行5列2', '行5列3'],
      ['行6列1', '行6列2', '行6列3'],
      ['行7列1', '行7列2', '行7列3'],
      ['行8列1', '行8列2', '行8列3'],
      ['行9列1', '行9列2', '行9列3'],
      ['行10列1', '行10列2', '行10列3'],
    ],
    index: true,
    align: ['center'],
    columnWidth: [50],
  });

  const config1 = reactive({
    data: [10.23],
  });
  const config2 = reactive({
    data: [20.23],
  });

  const url1 = ref(new URL('./images/chuansongdai.png', import.meta.url).href);
  const url2 = ref(new URL('./images/chuansongdai.png', import.meta.url).href);
  const url3 = ref(new URL('./images/chuansongdai.png', import.meta.url).href);
  const url4 = ref(new URL('./images/chuansongdai.png', import.meta.url).href);
  const url5 = ref(new URL('./images/chuansongdai.png', import.meta.url).href);
  const shexiangUrl1 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl2 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl3 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl4 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl5 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl6 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl7 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl8 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl9 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl10 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const warnUrl4 = ref(new URL('./images/正常.png', import.meta.url).href);
  const shexiangUrl12 = ref(new URL('./images/摄像头正常.png', import.meta.url).href);
  const shexiangUrl13 = ref(new URL('./images/电脑正常.png', import.meta.url).href);

  const DiannaoUrl1 = ref(new URL('./images/电脑正常.png', import.meta.url).href);
  const DiannaoUrl2 = ref(new URL('./images/电脑正常.png', import.meta.url).href);
  const DiannaoUrl3 = ref(new URL('./images/电脑正常.png', import.meta.url).href);
  const DiannaoUrl4 = ref(new URL('./images/电脑正常.png', import.meta.url).href);
  const warnUrl1 = ref(new URL('./images/正常.png', import.meta.url).href);
  const warnUrl2 = ref(new URL('./images/正常.png', import.meta.url).href);
  const warnUrl3 = ref(new URL('./images/正常.png', import.meta.url).href);
  const warnUrl5 = ref(new URL('./images/正常.png', import.meta.url).href);
  const zhujiUrl = ref(new URL('./images/主机正常.png', import.meta.url).href);

  const videoUrl = ref(
    new URL('http://210.30.96.101:30110/raw/modelAlert/1.mp4', import.meta.url).href,
  );

  const imageurl = ref(
    encodeURI(
      'http://210.30.96.101:30110/raw/subsystemAlert/综采/皮带纵撕/摄像头 1/date-20231207/144448366hot12345.jpg',
    ),
  );
  const visible = ref(false);
  const visible1 = ref(false);
  const zongsiInfo = ref('上仓胶带机纵撕');
  const name = ref('传送带一号每时煤量图');
  const isBe = ref(true);

  const a = ref();

  // 瞬时煤流信息
  const kamikuraInfo = ref([]);
  const mainWellInfo = ref([]);
  const mainTransInfo = ref([]);
  const generalMiningInfo = ref([]);
  const diggingInfo = ref([]);
  const selectInfo = ref([]);
  const SelectName = ref('综采摄像头选择');

  // 皮带信息
  const kamikura = ref([]);
  const mainWell = ref([]);
  const mainTrans = ref([]);
  const generalMining = ref([]);
  const digging = ref([]);
  const pidai = ref([]);
  const showInfo = ref([]);

  // 报警信息
  const kamikuraWarnInfo = ref([]);
  const mainWellWarnInfo = ref([]);
  const mainTransWarnInfo = ref([]);
  const generalMiningWarnInfo = ref([]);
  const diggingWarnInfo = ref([]);

  const kamikuraHourInfo = ref([]);
  const mainWellHourInfo = ref([]);
  const mainTransHourInfo = ref([]);
  const generalMiningHourInfo = ref([]);
  const diggingHourInfo = ref([]);

  const isBe1 = ref(true);
  const camera = ref({
    monitorName: '摄像头1',
    scene: '上仓',
    algName: '瞬时煤流',
    customInfo: {
      momentVolume: 2.5,
      fullload: 80.01,
    },
  });

  const warnInfoData = ref({
    tearLength: 10,
    tearWidth: 1,
    tearDeep: 10,
    tearHight: 1,
  });

  const cellStyle = ref((data) => {
    if (data.row / 2 === 0) {
      return {
        color: '#ffffff',
        textAlign: 'center',
        background: '#262626',
        fontWeight: 700,
      };
    } else {
      return {
        color: '#ffffff',
        textAlign: 'center',
        background: '#171b2c',
        fontWeight: 700,
      };
    }
  });
  const warningInfo = ref([]);

  const computerInfo = ref([
    [150.12, 130.25, 160.31, 180.29, 160.23, 170.32],
    [123.45, 132.54, 156.37, 142.17, 172.18, 162.19],
    [133.22, 144.88, 150.77, 160.23, 166.54, 173.14],
    [173.14, 166.54, 160.23, 150.77, 144.88, 133.22],
    [7, 10, 7, 4, 6, 8],
  ]);

  let internalInstance = getCurrentInstance();
  let echarts = internalInstance.appContext.config.globalProperties.$echarts;

  const option = ref({
    title: [
      {
        text: '综采工作面每时煤量图',
        left: '20%',
        position: 'middle',
        textStyle: {
          fontSize: 24,
          color: '#7fbbff',
        },
      },
    ],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        axisTick: {
          alignWithLabel: true,
        },
      },
    ],
    yAxis: [
      {
        type: 'value',
      },
    ],
    series: [
      {
        name: 'Direct',
        type: 'bar',
        barWidth: '60%',
        data: [10, 52, 200, 334, 390, 330, 220],
        itemStyle: {
          normal: {
            label: {
              show: true,
            },
            lineStyle: {
              color: '#5555ff',
            },
          },
        },
      },
    ],
  });
  // const option = ref({
  //   title: {
  //     text: name.value,
  //     left: '20%',
  //     textStyle: {
  //       fontSize: 24,
  //       color: '#7fbbff',
  //     },
  //   },
  //   grid: {
  //     left: '4%',
  //     right: '4%',
  //     bottom: '3%',
  //     containLabel: true,
  //   },
  //   xAxis: {
  //     name: '时间',
  //     type: 'category',
  //     boundaryGap: false,
  //     data: ['12:20', '13:20', '14:20', '15:20', '16:20', '17:20'],
  //   },
  //   yAxis: {
  //     name: '立方分米',
  //     type: 'value',
  //   },
  //   series: [
  //     {
  //       type: 'line',
  //       stack: 'Total',
  //       data: [150.23, 140.25, 153.76, 161.99, 152.14, 143.27],
  //       itemStyle: {
  //         normal: {
  //           color: 'red',
  //           label: {
  //             show: true,
  //           },
  //           lineStyle: {
  //             color: '#5555ff',
  //           },
  //         },
  //       },
  //       symbolSize: 5,
  //       smooth: true,
  //       lineStyle: {
  //         color: 'purple',
  //       },
  //     },
  //   ],
  // });

  onMounted(() => {
    const dom = document.getElementById('myChart');
    const myChart = echarts.init(dom, 'dark'); // 初始化echarts实例
    // 设置实例参数
    myChart.setOption(option.value);
    getInstantaneData();
    getHourData();
    getBeltTearing();
    getGearControl();
    getHourlyCoalFlowOverTime();
  });

  async function getHourData(params) {
    a.value = await maHttp.get(
      {
        url: 'coalflow/getHourlyCoalFlow',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );

    for (let i = 0; i < a.value.length; i++) {
      a.value[i].scene === '上仓'
        ? kamikuraHourInfo.value.push(a.value[i])
        : a.value[i].scene === '主进'
        ? mainWellHourInfo.value.push(a.value[i])
        : a.value[i].scene === '主运'
        ? mainTransHourInfo.value.push(a.value[i])
        : a.value[i].scene === '综采'
        ? generalMiningHourInfo.value.push(a.value[i])
        : diggingHourInfo.value.push(a.value[i]);
    }
    for (let i = 0; i < generalMiningHourInfo.value.length; i++) {
      selectInfo.value[i] = generalMiningHourInfo.value[i];
    }
    console.log(generalMiningHourInfo.value);
  }
  const b = ref([]);

  async function getInstantaneData(params) {
    b.value = await maHttp.get(
      {
        url: 'coalflow/getInstantaneousCoalFlow',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    for (let i = 0; i < b.value.length; i++) {
      b.value[i].scene === '上仓'
        ? kamikuraInfo.value.push(b.value[i])
        : b.value[i].scene === '主进'
        ? mainWellInfo.value.push(b.value[i])
        : b.value[i].scene === '主运'
        ? mainTransInfo.value.push(b.value[i])
        : b.value[i].scene === '综采'
        ? generalMiningInfo.value.push(b.value[i])
        : diggingInfo.value.push(b.value[i]);
    }
    for (let i = 0; i < generalMiningInfo.value.length; i++) {
      selectInfo.value[i] = generalMiningInfo.value[i];
    }
  }

  async function getHourlyCoalFlowOverTime(params) {
    const d = await maHttp.get(
      {
        url: 'coalflow/getHourlyCoalFlowOvertime',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    console.log(d);
  }

  async function getBeltTearing(params) {
    config.data = [];
    const v = await maHttp.get(
      {
        url: 'coalflow/getBeltTearing',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    v.forEach((index) => {
      index.status === 1
        ? (index.status = '已处理')
        : index.status === 2
        ? (index.status = '已忽略')
        : (index.status = '未处理');
    });
    for (let i = 0; i < v.length; i++) {
      config.data.push([v[i].createTime, v[i].description, v[i].status]);
    }

    for (let i = 0; i < v.length; i++) {
      v[i].scene === '上仓'
        ? kamikuraWarnInfo.value.push(v[i])
        : v[i].scene === '主进'
        ? mainWellWarnInfo.value.push(v[i])
        : v[i].scene === '主运'
        ? mainTransWarnInfo.value.push(v[i])
        : v[i].scene === '综采'
        ? generalMiningWarnInfo.value.push(v[i])
        : diggingWarnInfo.value.push(v[i]);
    }
    getDemo1();
  }

  async function getGearControl(params) {
    pidai.value = await maHttp.get(
      {
        url: 'coalflow/getGearControl',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    for (let i = 0; i < pidai.value.length; i++) {
      pidai.value[i].scene === '上仓'
        ? kamikura.value.push(pidai.value[i])
        : pidai.value[i].scene === '主进'
        ? mainWell.value.push(pidai.value[i])
        : pidai.value[i].scene === '主运'
        ? mainTrans.value.push(pidai.value[i])
        : pidai.value[i].scene === '综采'
        ? generalMining.value.push(pidai.value[i])
        : digging.value.push(pidai.value[i]);
    }
    getDemo();
    showInfo.value.push(kamikura.value[0]);
    showInfo.value.push(mainWell.value[0]);
    showInfo.value.push(mainTrans.value[0]);
    showInfo.value.push(generalMining.value[0]);
    showInfo.value.push(digging.value[0]);
  }
  function getDemo() {
    if (kamikura.value.length === 0) {
      kamikura.value.push({
        scene: '上仓',
        modelName: '档位调速',
        data: { coalFlow: 'null', gear: 'null', speed: 'null', fullLoadRatio: 'null' },
      });
    }
    if (mainWell.value.length === 0) {
      mainWell.value.push({
        scene: '主井',
        modelName: '档位调速',
        data: { coalFlow: 'null', gear: 'null', speed: 'null', fullLoadRatio: 'null' },
      });
    }
    if (mainTrans.value.length === 0) {
      mainTrans.value.push({
        scene: '主运',
        modelName: '档位调速',
        data: { coalFlow: 'null', gear: 'null', speed: 'null', fullLoadRatio: 'null' },
      });
    }
    if (generalMining.value.length === 0) {
      generalMining.value.push({
        scene: '综采',
        modelName: '档位调速',
        data: { coalFlow: 'null', gear: 'null', speed: 'null', fullLoadRatio: 'null' },
      });
    }
    if (digging.value.length === 0) {
      digging.value.push({
        scene: '掘进',
        modelName: '档位调速',
        data: { coalFlow: 'null', gear: 'null', speed: 'null', fullLoadRatio: 'null' },
      });
    }
  }

  function changeInfo(e) {
    console.log(e);
    videoUrl.value = 'http://210.30.96.101:30110/raw/' + e.videoPath;
    console.log(videoUrl.value);
    camera.value.monitorName = e.monitorName;
    camera.value.scene = e.scene;
    camera.value.algName = e.modelName;
    camera.value.customInfo.momentVolume = e.data.momentVolume;
    camera.value.customInfo.fullload = e.data.fullload;
    config.data[0] = parseFloat(e.data.fullload);
  }

  function close() {
    visible.value = false;
  }

  function kamikuraChange() {
    selectInfo.value = [];
    SelectName.value = '上仓摄像头选择';
    for (let i = 0; i < kamikuraInfo.value.length; i++) {
      selectInfo.value[i] = kamikuraInfo.value[i];
    }
  }
  function mainWellChange() {
    selectInfo.value = [];
    SelectName.value = '主井摄像头选择';
    for (let i = 0; i < mainWellInfo.value.length; i++) {
      selectInfo.value[i] = mainWellInfo.value[i];
    }
  }
  function mainTransChange() {
    selectInfo.value = [];
    SelectName.value = '主运摄像头选择';
    for (let i = 0; i < mainTransInfo.value.length; i++) {
      selectInfo.value[i] = mainTransInfo.value[i];
    }
  }

  function generalMiningchange() {
    selectInfo.value = [];
    SelectName.value = '综采摄像头选择';
    for (let i = 0; i < generalMiningInfo.value.length; i++) {
      selectInfo.value[i] = generalMiningInfo.value[i];
    }
  }

  function diggingchange() {
    selectInfo.value = [];
    SelectName.value = '掘进摄像头选择';
    for (let i = 0; i < diggingInfo.value.length; i++) {
      selectInfo.value[i] = diggingInfo.value[i];
    }
  }

  //有bug
  function search(e) {
    imageurl.value = 'http://210.30.96.101:30110/raw/' + e.row.imagePath;
    console.log(imageurl.value);
    warnInfoData.value = e.row.data;
    visible1.value = true;
  }

  function getComputerInfo() {
    option.value.title[0].text = name.value;
    const dom = document.getElementById('myChart');
    const myChart = echarts.init(dom); // 初始化echarts实例
    myChart.setOption(option.value);
  }

  function getComputerInfo1() {
    name.value = '上仓胶带机每时煤量图';

    option.value.xAxis[0].data = [];
    option.value.series[0].data = [];
    for (let i = 0; i < kamikuraHourInfo.value.length; i++) {
      option.value.series[0].data.push(kamikuraHourInfo.value[i].data.totalVolume);
      option.value.xAxis[0].data.push(kamikuraHourInfo.value[i].monitorName);
    }
    getComputerInfo();
  }

  function getComputerInfo2() {
    name.value = '主井胶带机每时煤量图';

    option.value.xAxis[0].data = [];
    option.value.series[0].data = [];
    for (let i = 0; i < mainWellHourInfo.value.length; i++) {
      option.value.series[0].data.push(mainWellHourInfo.value[i].data.totalVolume);
      option.value.xAxis[0].data.push(mainWellHourInfo.value[i].monitorName);
    }
    getComputerInfo();
  }

  function getComputerInfo3() {
    name.value = '主运胶带机每时煤量图';
    option.value.xAxis[0].data = [];
    option.value.series[0].data = [];
    for (let i = 0; i < mainTransHourInfo.value.length; i++) {
      option.value.series[0].data.push(mainTransHourInfo.value[i].data.totalVolume);
      option.value.xAxis[0].data.push(mainTransHourInfo.value[i].monitorName);
    }
    getComputerInfo();
  }

  function getComputerInfo4() {
    name.value = '综采工作面每时煤量图';
    option.value.xAxis[0].data = [];
    option.value.series[0].data = [];
    for (let i = 0; i < generalMiningHourInfo.value.length; i++) {
      option.value.series[0].data.push(generalMiningHourInfo.value[i].data.totalVolume);
      option.value.xAxis[0].data.push(generalMiningHourInfo.value[i].monitorName);
    }
    getComputerInfo();
  }

  function getComputerInfo5() {
    name.value = '掘进工作面每时煤量图';
    option.value.xAxis[0].data = [];
    option.value.series[0].data = [];
    for (let i = 0; i < diggingHourInfo.value.length; i++) {
      option.value.series[0].data.push(diggingHourInfo.value[i].data.totalVolume);
      option.value.xAxis[0].data.push(diggingHourInfoo.value[i].monitorName);
    }
    getComputerInfo();
  }

  function getcolor(scope) {
    if (scope.row.address == '未处理') {
      return 'warning';
    } else if (scope.row.address == '已忽略') {
      return 'success';
    } else {
      return 'primary';
    }
  }

  function WarnningChange1() {
    zongsiInfo.value = '上仓胶带机纵撕';
    warningInfo.value = kamikuraWarnInfo.value;
    visible.value = 'ture';
  }
  function WarnningChange2() {
    zongsiInfo.value = '主井胶带机纵撕';
    warningInfo.value = mainWellInfo.value;
    visible.value = 'ture';
  }
  function WarnningChange3() {
    zongsiInfo.value = '主运胶带机纵撕';
    warningInfo.value = mainTransWarnInfo.value;
    visible.value = 'ture';
  }
  function WarnningChange4() {
    zongsiInfo.value = '综采胶带机纵撕';
    warningInfo.value = generalMiningWarnInfo.value;
    visible.value = 'ture';
  }

  function WarnningChange5() {
    zongsiInfo.value = '掘进胶带机纵撕';
    warningInfo.value = diggingWarnInfo.value;
    visible.value = 'ture';
  }

  function getDemo1() {
    for (let i = 0; i < kamikuraWarnInfo.value.length; i++) {
      if (kamikuraWarnInfo.value[i].status === '未处理') {
        warnUrl1.value = new URL('./images/异常.png', import.meta.url).href;
      }
    }
    for (let i = 0; i < mainWellWarnInfo.value.length; i++) {
      if (mainWellWarnInfo.value[i].status === '未处理') {
        warnUrl2.value = new URL('./images/异常.png', import.meta.url).href;
      }
    }
    for (let i = 0; i < mainTransWarnInfo.value.length; i++) {
      if (mainTransWarnInfo.value[i].status === '未处理') {
        warnUrl3.value = new URL('./images/异常.png', import.meta.url).href;
      }
    }
    for (let i = 0; i < generalMiningWarnInfo.value.length; i++) {
      if (generalMiningWarnInfo.value[i].status === '未处理') {
        warnUrl4.value = new URL('./images/异常.png', import.meta.url).href;
      }
    }
    for (let i = 0; i < diggingWarnInfo.value.length; i++) {
      if (diggingWarnInfo.value[i].status === '未处理') {
        warnUrl5.value = new URL('./images/异常.png', import.meta.url).href;
      }
    }
  }
</script>
<style>
  . {
    margin: 0;
    padding: 0;
  }

  .constain {
    background-color: rgba(248, 248, 248, 0.993);
    height: 100vh;
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    background-size: 100% 100%;
    background-attachment: fixed;
  }

  .echarts1 {
    position: relative;
    top: 10%;
  }

  .el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell {
    background-color: rgb(35, 145, 130) !important;
  }

  .el-dialog__body {
    padding: 0px !important;
  }

  .el-dialog {
    --el-dialog-padding-primary: 0px !important;
  }

  .box-item {
    width: 60px;
    margin-top: 10px;
  }

  .share_atooltip .el-tooltip__popper[x-placement^='right'] .popper__arrow {
    border-right-color: #00feff;
  }
  .share_atooltip .el-tooltip__popper[x-placement^='right'] .popper__arrow ::after {
    border-right-color: #00feff;
  }
  .share_atooltip {
    background: #2b3b4e !important;
  }

  .test:active {
    border: 2px;
    background-color: aquamarine;
  }
  .fontStyle {
    color: #00ddeb;
    font-size: 20px;
  }
</style>
